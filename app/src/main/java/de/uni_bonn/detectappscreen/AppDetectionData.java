package de.uni_bonn.detectappscreen;

import android.content.Context;
import android.os.Environment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Collator;
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
     * Reads a JSON file from the external storage public directory with the given sub-directory and filename
     * @param subDirectory    Sub-directory to use
     * @param filename        Filename of the file to read
     * @return A JSONObject with the file's contents
     */
    public static JSONObject readJSONFile(String subDirectory, String filename) {
        JSONObject result = null;

        File file = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), subDirectory + "/" + filename);
        try (InputStream is = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            result = new JSONObject(stringBuilder.toString());
        } catch (FileNotFoundException e) {
            Log.e("WDebug", "File not found: " + file.getAbsolutePath());
            Log.e("WDebug", e.getMessage());
        } catch (JSONException e) {
            Log.e("WDebug", "Unable to create JSONObject from file (" + file.getAbsolutePath() + "): " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

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
        this.layoutsToLoad = layouts;
        this.reverseMapToLoad = reverseMap;
        this.currentAppUsageData = new AppUsageData(packageName);
        this.context = context;
    }

    /**
     * Creates an AppDetectionData object using the given parameters, automatically loads
     * the layouts and reverse map files from the according directory
     * @param packageName
     */
    public AppDetectionData(String packageName, Context context) {
        this.finishedLoading = false;
        this.performLayoutChecks = false;
        this.performOnClickChecks = false;
        this.packageName = packageName;
        this.layoutsToLoad = AppDetectionData.readJSONFile(packageName, "layouts.json");
        this.reverseMapToLoad = AppDetectionData.readJSONFile(packageName, "reverseMap.json");
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
     * Loads the app detection data, i.e. constructs hashmaps needed for realtime detection
     */
    public void load(boolean performLayoutChecks, boolean performOnClickChecks) {
        this.performLayoutChecks = performLayoutChecks;
        this.performOnClickChecks = performOnClickChecks;
        boolean finishedLoading = true;

        if (performLayoutChecks) {
            boolean hashMapsLoaded = buildHashMaps(this.layoutsToLoad, this.reverseMapToLoad);
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

        if (shallPerformLayoutChecks(event))
            checkLayouts(event.getSource(), activity);

        if (shallPerformOnClickChecks(event)) {
            Set<ClickedEventData> clickedEventData = checkOnClickEvents(event.getSource(), rootNodeInfo);
            Log.i("Clicked events", clickedEventData.toString());
        }

        //boolean shallLog = currentAppUsageData.addDataEntry(activity, recognizedLayouts);
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
     * @param activity    Current activity to add to the AppUsageDataEntry
     */
    private void checkLayouts(AccessibilityNodeInfo source, String activity) {
        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo(source);

        Set<String> androidIDsOnScreen = androidIDsOnScreen(rootNodeInfo);
        Set<String> possibleLayouts = possibleLayouts(androidIDsOnScreen);
        Set<String> recognizedLayouts = recognizedLayouts(androidIDsOnScreen, possibleLayouts);

        boolean shallLog = currentAppUsageData.addDataEntry(activity, recognizedLayouts);
        if (shallLog)
            Log.i("Recognized Layouts", recognizedLayouts.toString());
    }

    /**
     * Handles on-click events, i.e. creates a data entry showing what element was clicked (todo)
     * @param source    Source node info
     * @return todo: subject to change
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
     * Constructs hashmaps (layout -> (layout identifiers))
     * and (android ID -> (possible layouts))
     * @param layouts       Layout definitions in JSON
     * @param reverseMap    Reverse map in JSON
     * @return True if building the hashmaps was successful, false otherwise
     */
    private boolean buildHashMaps(JSONObject layouts, JSONObject reverseMap) {
        Log.i("WDebug", "Building hashmaps...");
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
                Set<String> layoutsAssociated = new TreeSet<>(Collator.getInstance());
                for (int j = 0; j < layoutsAssociatedAsArray.length(); ++j)
                    layoutsAssociated.add(layoutsAssociatedAsArray.getString(j));

                this.reverseMap.put(androidID, layoutsAssociated);
            }
        } catch (JSONException e) {
            Log.e("WDebug", "Error reading from JSONObject: " + e.getMessage());
        }

        Log.i("WDebug", "Done building Hashmaps.");

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
        Set<String> androidIDsOnScreen = new TreeSet<>(Collator.getInstance());

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
        Set<String> possibleLayouts = new TreeSet<>(Collator.getInstance());
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
        Set<String> recognizedLayouts = new TreeSet<>(Collator.getInstance());
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
