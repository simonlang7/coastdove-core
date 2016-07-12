package de.uni_bonn.detectappscreen;

import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Data needed for detecting layouts in an associated app. Layouts are identified by certain (if possible unique)
 * identifiers their XML files contain. For now, only android:id properties are considered. There can be only one
 * android ID that uniquely identifies a layout, or a set of several android IDs, though as few as possible are
 * favored.
 */
public class AppDetectionData {

    /** Indicates whether the data has finished loading */
    private boolean finishedLoading;
    /** Layout definitions as JSON, to be used for building hashmaps */
    private JSONObject layoutsToLoad;
    /** Reverse map, mapping from sets of android IDs to layouts that can possibly be identified */
    private JSONObject reverseMapToLoad;

    /** Name of the package associated, i.e. the app that can be detected */
    private String packageName;
    /** Indicates whether the hash maps (layoutIdentificationMap and reverseMap) have been loaded or not */
    private boolean hashMapsLoaded;
    /** Hashmap mapping from each layout to a set of android IDs that identify the layout*/
    private Map<String, LayoutIdentification> layoutIdentificationMap;
    /** Reverse hashmap, mapping from sets of android IDs to layouts that can possibly be identified */
    private Map<String, Set<String>> reverseMap;

    private boolean performLayoutChecks;
    private boolean performOnClickChecks;

    /** Usage data collected for this session (that starts when the app is opened and ends when it's closed) */
    private AppUsageData currentAppUsageData;

    /** Application context needed to scan files */
    private Context context;

    /**
     * Creates an AppDetectionData object using the given parameters
     * @param packageName    Name of the package for app detection
     * @param layouts        Layouts in JSON format
     * @param reverseMap     Reverse map in JSON format
     */
    public AppDetectionData(String packageName, JSONObject layouts, JSONObject reverseMap, Context context) {
        this.finishedLoading = false;
        this.performLayoutChecks = false;
        this.performOnClickChecks = false;
        this.packageName = packageName;
        this.hashMapsLoaded = false;
        this.layoutsToLoad = layouts;
        this.reverseMapToLoad = reverseMap;
        this.layoutIdentificationMap = null;
        this.reverseMap = null;
        this.currentAppUsageData = new AppUsageData(packageName);
        this.context = context;
    }

    /**
     * Creates an AppDetectionData object using the given parameters, automatically loads
     * the layouts and reverse map files from the according directory
     * @param packageName    Name of the package for app detection
     */
    public AppDetectionData(String packageName, Context context) {
        this.finishedLoading = false;
        this.performLayoutChecks = false;
        this.performOnClickChecks = false;
        this.packageName = packageName;
        this.hashMapsLoaded = false;
        this.layoutsToLoad = FileHelper.readJSONFile(packageName, "layouts.json");
        this.reverseMapToLoad = FileHelper.readJSONFile(packageName, "reverseMap.json");
        this.layoutIdentificationMap = null;
        this.reverseMap = null;
        this.currentAppUsageData = new AppUsageData(packageName);
        this.context = context;
    }

    /**
     * Returns true iff the app detection data has finished loading
     */
    public boolean isFinishedLoading() {
        return this.finishedLoading;
    }

    /**
     * Loads the app detection data, i.e. constructs hash maps needed for real-time detection
     */
    public void load(boolean performLayoutChecks, boolean performOnClickChecks) {
        this.performLayoutChecks = performLayoutChecks;
        this.performOnClickChecks = performOnClickChecks;
        boolean finishedLoading = true;

        if (performLayoutChecks && !this.hashMapsLoaded) {
            // Are the hash maps ready in binary format? Load them.
            this.hashMapsLoaded = buildHashMapsFromBinary();
            // Otherwise, revert to building them from JSON files
            if (!this.hashMapsLoaded) {
                this.hashMapsLoaded = buildHashMapsFromJSON(this.layoutsToLoad, this.reverseMapToLoad);

                // And save them as binaries
                if (!FileHelper.fileExists(getPackageName(), "layoutsMap.bin"))
                    FileHelper.writeHashMap(this.context, (HashMap)this.layoutIdentificationMap, getPackageName(), "layoutsMap.bin");
                if (!FileHelper.fileExists(getPackageName(), "reverseMap.bin"))
                    FileHelper.writeHashMap(this.context, (HashMap)this.reverseMap, getPackageName(), "reverseMap.bin");
            }

            finishedLoading = finishedLoading && hashMapsLoaded;
        }

        this.finishedLoading = finishedLoading;
    }

    /**
     * Performs necessary operations to detect the layouts currently being used by the according app,
     * and/or gesture events and/or on-click events
     * @param event       Accessibility event that was triggered
     * @param activity    Current activity to add to the AppUsageDataEntry
     */
    public void performChecks(AccessibilityEvent event, AccessibilityNodeInfo rootNodeInfo, String activity) {
        if (!isFinishedLoading())
            return;

        if (shallPerformLayoutChecks(event)) {
            Set<String> recognizedLayouts = checkLayouts(event.getSource());

            boolean shallLog = currentAppUsageData.addLayoutDataEntry(activity, recognizedLayouts);
            if (shallLog)
                Log.i("Recognized layouts", recognizedLayouts.toString());
        }

        if (shallPerformOnClickChecks(event)) {
            Set<ClickedEventData> clickedEventData = checkOnClickEvents(event.getSource(), rootNodeInfo);

            boolean shallLog = currentAppUsageData.addClickDataEntry(activity, clickedEventData);
            if (shallLog)
                Log.i("Clicked events", clickedEventData.toString());
        }

    }

    /**
     * Writes the current app usage data to a file and swaps it for an empty new one,
     * shall be called whenever the app to be detected is exited
     */
    public void saveAppUsageData() {
        writeAppUsageData();
        currentAppUsageData = new AppUsageData(this.packageName);
    }

    /**
     * Returns the package name of the app to be detected by these data
     */
    public String getPackageName() {
        return packageName;
    }

    public boolean getPerformLayoutChecks() {
        return this.performLayoutChecks;
    }

    public boolean getPerformOnClickChecks() {
        return this.performOnClickChecks;
    }

    /**
     * Returns true iff a layout comparison shall be performed
     */
    private boolean shallPerformLayoutChecks(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            return false;
        if (event.getSource() == null)
            return false;
        if (!performLayoutChecks)
            return false;

        return true;
    }

    /**
     * Returns true iff an on-click event check shall be performed
     */
    private boolean shallPerformOnClickChecks(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED)
            return false;
        if (event.getSource() == null)
            return false;
        if (!performOnClickChecks)
            return false;

        return true;
    }

    /**
     * Performs necessary operations to detect the layouts currently being used
     * @param source      Source node info
     * @return Detected layouts
     */
    private Set<String> checkLayouts(AccessibilityNodeInfo source) {
        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo(source);

        Set<String> androidIDsOnScreen = androidIDsOnScreen(rootNodeInfo);
        Set<String> possibleLayouts = possibleLayouts(androidIDsOnScreen);
        return recognizedLayouts(androidIDsOnScreen, possibleLayouts);
    }

    /**
     * Handles on-click events, i.e. creates a data entry showing what element was clicked (todo)
     * @param source    Source node info
     * @return Data regarding this on-click event
     */
    private Set<ClickedEventData> checkOnClickEvents(AccessibilityNodeInfo source, AccessibilityNodeInfo rootNodeInfo) {
        Set<ClickedEventData> result = new CopyOnWriteArraySet<>();
        //AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo(source);
        AccessibilityNodeInfo nodeInfo = null;

        // Try to find the node info with the same text as the source.
        // For some reason, the source does not contain any View ID information,
        // so we need to find the same node info again in order to get the view ID.
        if (source.getText() != null)
            nodeInfo = findNodeInfo(rootNodeInfo, source.getText().toString(),
                    source.getClassName() != null ? source.getClassName().toString() : null);
        if (nodeInfo == null)
            nodeInfo = source;

        if (nodeInfo.getViewIdResourceName() != null || nodeInfo.getText() != null) {
            result.add(new ClickedEventData(nodeInfo));
        }
        else {
            // If that didn't work, try to get the view IDs of the children
            if (source.getChildCount() > 0) {
                for (int i = 0; i < source.getChildCount(); ++i) {
                    AccessibilityNodeInfo child = source.getChild(i);
                    if (child.getViewIdResourceName() != null)
                        result.add(new ClickedEventData(child));
                }
            }
            // ... or the parent, if we still haven't had any luck
            if (result.size() == 0) {
                AccessibilityNodeInfo parent = source.getParent();
                if (parent != null && parent.getViewIdResourceName() != null)
                    result.add(new ClickedEventData(parent));
            }
        }

        return result;
    }

    private AccessibilityNodeInfo findNodeInfo(AccessibilityNodeInfo startNodeInfo, String matchingText, String matchingClassName) {
        if (matchingText == null)
            return null;

        AccessibilityNodeInfo currentNodeInfo = startNodeInfo;
        List<AccessibilityNodeInfo> nodeInfos = new LinkedList<>();
        nodeInfos.add(currentNodeInfo);

        // breadth-first search for a node info with matching text and/or matching class name
        while (nodeInfos.size() > 0) {
            currentNodeInfo = nodeInfos.get(0);
            if (currentNodeInfo != null) {
                boolean textMatch = currentNodeInfo.getText() != null && matchingText.equals(currentNodeInfo.getText().toString());
                boolean classNameMatch = matchingClassName == null
                        || (currentNodeInfo.getClassName() != null && matchingClassName.equals(currentNodeInfo.getClassName().toString()));

                if (textMatch && classNameMatch)
                    return currentNodeInfo;

                // add children
                for (int i = 0; i < currentNodeInfo.getChildCount(); ++i)
                    nodeInfos.add(currentNodeInfo.getChild(i));
            }


            nodeInfos.remove(0);
        }

        return null;
    }

    /**
     * Writes the current app usage data to a file
     */
    private void writeAppUsageData() {
        AppUsageData appUsageData = currentAppUsageData;
        new Thread(new AppUsageDataWriter("AppUsageData", appUsageData, this.context)).start();
    }

    /**
     * Constructs hash maps (layout -> (layout identifiers))
     * and (android ID -> (possible layouts))
     * from serialized objects
     * @return True if building the hash maps was successful, false otherwise
     */
    private boolean buildHashMapsFromBinary() {
        // TODO: un-hardcode
        if (FileHelper.fileExists(getPackageName(), "layoutsMap.bin") &&
                FileHelper.fileExists(getPackageName(), "reverseMap.bin")) {
            Log.v("AppDetectionData", "Building hash maps from binary...");
            this.layoutIdentificationMap =
                    (Map<String, LayoutIdentification>)FileHelper.readHashMap(getPackageName(), "layoutsMap.bin");
            this.reverseMap = (Map<String, Set<String>>)FileHelper.readHashMap(getPackageName(), "reverseMap.bin");
            Log.v("AppDetectionData", "Finished building hash maps from binary");
            return this.layoutIdentificationMap != null && this.reverseMap != null;
        }

        return false;
    }

    /**
     * Constructs hash maps (layout -> (layout identifiers))
     * and (android ID -> (possible layouts))
     * from the given JSON objects
     * @param layouts       Layout definitions in JSON
     * @param reverseMap    Reverse map in JSON
     * @return True if building the hash maps was successful, false otherwise
     */
    private boolean buildHashMapsFromJSON(JSONObject layouts, JSONObject reverseMap) {
        Log.v("AppDetectionData", "Building hash maps from JSON...");
        this.layoutIdentificationMap = new HashMap<>();
        this.reverseMap = new HashMap<>();

        try {
            JSONArray layoutsAsArray = layouts.getJSONArray("layoutDefinitions");
            for (int i = 0; i < layoutsAsArray.length(); ++i) {
                if (Thread.interrupted())
                    return false;

                JSONObject currentLayout = layoutsAsArray.getJSONObject(i);
                String name = currentLayout.getString("name");
                int ambiguity = currentLayout.getInt("ambiguity");
                LayoutIdentification layoutIdentification
                        = new LayoutIdentification(name, ambiguity, currentLayout.getJSONArray("layoutIdentifiers"));

                this.layoutIdentificationMap.put(name, layoutIdentification);
            }

            JSONArray reverseMapAsArray = reverseMap.getJSONArray("androidIDMap");
            for (int i = 0; i < reverseMapAsArray.length(); ++i) {
                if (Thread.interrupted())
                    return false;

                JSONObject currentElement = reverseMapAsArray.getJSONObject(i);
                String androidID = currentElement.getString("androidID");
                JSONArray layoutsAssociatedAsArray = currentElement.getJSONArray("layouts");
                Set<String> layoutsAssociated = new TreeSet<>(new CollatorWrapper());
                for (int j = 0; j < layoutsAssociatedAsArray.length(); ++j)
                    layoutsAssociated.add(layoutsAssociatedAsArray.getString(j));

                this.reverseMap.put(androidID, layoutsAssociated);
            }
        } catch (JSONException e) {
            Log.e("AppDetectionData", "Error reading from JSONObject: " + e.getMessage());
        }

        Log.v("AppDetectionData", "Finished building hash maps from JSON");

        return true;
    }

    /**
     * Returns the root of the tree of AccessibilityNodeInfos, needed for full traversal
     * @param sourceNodeInfo    NodeInfo that triggered the AccessibilityEvent
     */
    private AccessibilityNodeInfo getRootNodeInfo(AccessibilityNodeInfo sourceNodeInfo) {
        AccessibilityNodeInfo currentNodeInfo = sourceNodeInfo;

        while (currentNodeInfo != null && currentNodeInfo.getParent() != null)
            currentNodeInfo = currentNodeInfo.getParent();

        return currentNodeInfo;
    }

    /**
     * Returns a set of android IDs that occur on the current screen
     * @param startNodeInfo    Starting node from which to consider objects on screen (root node for all)
     */
    private Set<String> androidIDsOnScreen(AccessibilityNodeInfo startNodeInfo) {
        AccessibilityNodeInfo currentNodeInfo = startNodeInfo;
        Set<String> androidIDsOnScreen = new TreeSet<>(new CollatorWrapper());

        // Traverse tree downwards (breadth-first search)
        List<AccessibilityNodeInfo> nodeInfos = new LinkedList<>();
        nodeInfos.add(currentNodeInfo); // currently the root of the tree
        while (nodeInfos.size() > 0) {
            currentNodeInfo = nodeInfos.get(0);
            if (currentNodeInfo != null) {
                String viewIdResName = currentNodeInfo.getViewIdResourceName();
                String currentAndroidID = viewIdResName != null ? viewIdResName.replace(this.packageName + ":", "") : "";
                if (!currentAndroidID.equals("")) {
                    androidIDsOnScreen.add(currentAndroidID);
                }

                // add children, todo: add only one child if this is a list
                for (int i = 0; i < currentNodeInfo.getChildCount(); ++i)
                    nodeInfos.add(currentNodeInfo.getChild(i));
            }


            nodeInfos.remove(0);
        }

        return androidIDsOnScreen;
    }

    /**
     * Returns a set of layouts that can possibly be detected, given a set of androidIDs
     * @param androidIDs    set of android IDs detected on the screen
     */
    private Set<String> possibleLayouts(Set<String> androidIDs) {
        Set<String> possibleLayouts = new TreeSet<>(new CollatorWrapper());
        for (String androidIDOnScreen : androidIDs) {
            Set<String> currentPossibleLayouts = this.reverseMap.get(androidIDOnScreen);
            if (currentPossibleLayouts != null)
                possibleLayouts.addAll(currentPossibleLayouts);
        }

        return possibleLayouts;
    }

    /**
     * Returns the set of layouts recognized, given the set of android IDs detected on the screen,
     * and the set of possibly recognizable layouts
     * @param androidIDs         set of android IDs detected on the screen
     * @param possibleLayouts    set of possibly recognizable layouts
     */
    private Set<String> recognizedLayouts(Set<String> androidIDs, Set<String> possibleLayouts) {
        Set<String> recognizedLayouts = new TreeSet<>(new CollatorWrapper());
        for (String possibleLayout : possibleLayouts) {
            LayoutIdentification layout = this.layoutIdentificationMap.get(possibleLayout);
            for (Set<String> layoutIdentifierSet : layout.getLayoutIdentifiers()) {
                if (androidIDs.containsAll(layoutIdentifierSet)) {
                    recognizedLayouts.add(layout.getName());
                    break;
                }
            }
        }
        return recognizedLayouts;
    }
}
