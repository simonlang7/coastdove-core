package de.uni_bonn.detectappscreen;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Rect;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
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
    /** Layout definitions as JSON, to be used for building hash maps */
    private JSONObject layoutsToLoad;
    /** Reverse map, mapping from sets of android IDs to layouts that can possibly be identified */
    private JSONObject reverseMapToLoad;

    /** Name of the package associated, i.e. the app that can be detected */
    private String packageName;
    /** Indicates whether the hash maps (layoutIdentificationMap and reverseMap) have been loaded or not */
    private boolean hashMapsLoaded;
    /** Hash map mapping from each layout to a set of android IDs that identify the layout*/
    private Map<String, LayoutIdentification> layoutIdentificationMap;
    /** Reverse hash map, mapping from sets of android IDs to layouts that can possibly be identified */
    private Map<String, Set<String>> reverseMap;

    /** Indicates whether to perform layout checks or not */
    private boolean performLayoutChecks;
    /** Indicates whether to perform on-click checks or not */
    private boolean performOnClickChecks;

    /** Usage data collected for this session (that starts when the app is opened and ends when it's closed) */
    private AppUsageData currentAppUsageData;

    /** Application context needed to scan files */
    private Context context;
    /** Unique identifier for the notification progress bar */
    private int uid;

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
        this.uid = packageName.hashCode();
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
        this.uid = packageName.hashCode();
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
            // If the thread was interrupted, do not load anything else
            if (Thread.interrupted())
                return;

            // Otherwise, revert to building them from JSON files
            if (!this.hashMapsLoaded) {
                this.hashMapsLoaded = buildHashMapsFromJSON(this.layoutsToLoad, this.reverseMapToLoad);

                // And save them as binaries
                if (this.hashMapsLoaded && !FileHelper.fileExists(getPackageName(), "layoutsMap.bin"))
                    FileHelper.writeHashMap(this.context, (HashMap)this.layoutIdentificationMap, getPackageName(), "layoutsMap.bin");
                if (this.hashMapsLoaded && !FileHelper.fileExists(getPackageName(), "reverseMap.bin"))
                    FileHelper.writeHashMap(this.context, (HashMap)this.reverseMap, getPackageName(), "reverseMap.bin");
            }

            finishedLoading = hashMapsLoaded;
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

        if (shallPerformActivityChecks(event)) {
            boolean shallLog = currentAppUsageData.addActivityDataEntry(activity);
            if (shallLog)
                Log.i("Activity", activity);
        }

        if (shallPerformLayoutChecks(event)) {
            Set<String> recognizedLayouts = checkLayouts(event.getSource(), rootNodeInfo);

            boolean shallLog = currentAppUsageData.addLayoutDataEntry(activity, recognizedLayouts);
            if (shallLog)
                Log.i("Recognized layouts", recognizedLayouts.toString());
        }

        if (shallPerformScrollChecks(event)) {
            String scrolledElement = checkScrollEvent(event.getSource(), rootNodeInfo);

            boolean shallLog = currentAppUsageData.addScrollDataEntry(activity, scrolledElement);
            if (shallLog)
                Log.i("Scrolled element", scrolledElement);
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

    private boolean shallPerformActivityChecks(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            return false;

        return true;
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
     * Returns true iff a scroll check shall be performed
     */
    private boolean shallPerformScrollChecks(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_SCROLLED)
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
    private Set<String> checkLayouts(AccessibilityNodeInfo source, AccessibilityNodeInfo rootNodeInfo) {
        Set<String> androidIDsOnScreen = androidIDsOnScreen(rootNodeInfo);
        Set<String> possibleLayouts = possibleLayouts(androidIDsOnScreen);
        return recognizedLayouts(androidIDsOnScreen, possibleLayouts);
    }

    /**
     * Creates a data entry showing which element was scrolled
     * @return Data regarding the scroll event
     */
    private String checkScrollEvent(AccessibilityNodeInfo source, AccessibilityNodeInfo rootNodeInfo) {
        AccessibilityNodeInfo nodeInfo = findNodeInfo(source, rootNodeInfo);
        if (nodeInfo == null)
            nodeInfo = source;
        String scrolledElement = nodeInfo.getViewIdResourceName();
        return scrolledElement != null ? scrolledElement : "-";
    }

    /**
     * Handles on-click events, i.e. creates a data entry showing which element was clicked
     * @param source    Source node info
     * @return Data regarding this on-click event
     */
    private Set<ClickedEventData> checkOnClickEvents(AccessibilityNodeInfo source, AccessibilityNodeInfo rootNodeInfo) {
        Set<ClickedEventData> result = new CopyOnWriteArraySet<>();
        //AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo(source);

        // Try to find the node info with the same bounds as the source.
        // For some reason, the source does not contain any View ID information,
        // so we need to find the same node info again in order to get the view ID.
        AccessibilityNodeInfo subTree = findNodeInfo(source, rootNodeInfo);

        if (subTree == null)
            subTree = source;

        NodeInfoTraverser<ClickedEventData> traverser = new NodeInfoTraverser<>(subTree,
                new NodeInfoDataExtractor<ClickedEventData>() {
                    @Override
                    public ClickedEventData extractData(AccessibilityNodeInfo nodeInfo) {
                        return new ClickedEventData(nodeInfo);
                    }
                },
                new NodeInfoFilter() {
                    @Override
                    public boolean filter(AccessibilityNodeInfo nodeInfo) {
                        return nodeInfo != null &&
                                (nodeInfo.getViewIdResourceName() != null || nodeInfo.getText() != null);
                    }
                });

        result.addAll(traverser.getAllFiltered());

        // todo: do not add parent?
        // If we still didn't get any node with at least an ID or a text, add the source node's parent
        if (result.size() == 0) {
            AccessibilityNodeInfo parent = source.getParent();
            if (parent != null &&
                    (parent.getViewIdResourceName() != null
                    || parent.getText() != null))
                result.add(new ClickedEventData(parent));
        }

        return result;
    }

    /**
     * Tries to find a given node info in the given tree of node infos, comparing them
     * by their bounds only. This is needed for some events where the delivered source
     * node info does not contain any View ID information, but the entire tree of node
     * infos does. Finding the same node info in the tree solves this problem.
     * @param nodeInfoToFind    Node info to be found in the tree
     * @param nodeInfoTree      Tree in which to look for the node info
     * @return The node info found in the tree, or null if none was found
     */
    private AccessibilityNodeInfo findNodeInfo(AccessibilityNodeInfo nodeInfoToFind, AccessibilityNodeInfo nodeInfoTree) {
        Rect bounds = new Rect();
        nodeInfoToFind.getBoundsInParent(bounds);
        final Rect boundsInParent = new Rect(bounds);
        nodeInfoToFind.getBoundsInScreen(bounds);
        final Rect boundsInScreen = new Rect(bounds);

        NodeInfoTraverser<AccessibilityNodeInfo> nodeFinder = new NodeInfoTraverser<>(nodeInfoTree,
                new NodeInfoDataExtractor<AccessibilityNodeInfo>() {
                    @Override
                    public AccessibilityNodeInfo extractData(AccessibilityNodeInfo nodeInfo) {
                        // We need the node info directly
                        return nodeInfo;
                    }
                },
                new NodeInfoFilter() {
                    @Override
                    public boolean filter(AccessibilityNodeInfo nodeInfo) {
                        // But only the (first) one with matching bounds
                        if (nodeInfo == null)
                            return false;
                        Rect bounds = new Rect();
                        nodeInfo.getBoundsInParent(bounds);
                        if (!bounds.equals(boundsInParent))
                            return false;
                        nodeInfo.getBoundsInScreen(bounds);
                        if (!bounds.equals(boundsInScreen))
                            return false;

                        return true;
                    }
                });
        return nodeFinder.nextFiltered();
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

        NotificationManager notifyManager = (NotificationManager)this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context);
        builder.setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notification_loading_1) + " " + getPackageName()
                        + " " + context.getString(R.string.notification_loading_2))
                .setSmallIcon(R.drawable.notification_template_icon_bg)
                .setProgress(0, 0, true);
        if (FileHelper.fileExists(getPackageName(), "layoutsMap.bin") &&
                FileHelper.fileExists(getPackageName(), "reverseMap.bin")) {
            if (Thread.currentThread().isInterrupted()) {
                notifyManager.cancel(this.uid);
                return false;
            }

            notifyManager.notify(this.uid, builder.build());

            Log.v("AppDetectionData", "Building hash maps from binary...");
            this.layoutIdentificationMap =
                    (Map<String, LayoutIdentification>)FileHelper.readHashMap(getPackageName(), "layoutsMap.bin");

            if (Thread.currentThread().isInterrupted()) {
                notifyManager.cancel(this.uid);
                return false;
            }

            this.reverseMap = (Map<String, Set<String>>)FileHelper.readHashMap(getPackageName(), "reverseMap.bin");
            Log.v("AppDetectionData", "Finished building hash maps from binary");

            if (Thread.currentThread().isInterrupted()) {
                notifyManager.cancel(this.uid);
                return false;
            }

            builder.setContentText(context.getString(R.string.notification_finished_loading_1) + " " + getPackageName()
                    + " " + context.getString(R.string.notification_finished_loading_2));
            builder.setProgress(0, 0, false);
            notifyManager.notify(this.uid, builder.build());

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

        NotificationManager notifyManager = (NotificationManager)this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context);
        builder.setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notification_loading_1) + " " + getPackageName()
                        + " " + context.getString(R.string.notification_loading_2))
                .setSmallIcon(R.drawable.notification_template_icon_bg)
                .setProgress(0, 0, true);

        try {
            JSONArray layoutsAsArray = layouts.getJSONArray("layoutDefinitions");
            JSONArray reverseMapAsArray = reverseMap.getJSONArray("androidIDMap");
            this.layoutIdentificationMap = new HashMap<>(layoutsAsArray.length(), 1.0f);
            this.reverseMap = new HashMap<>(reverseMapAsArray.length(), 1.0f);
            //int maxProgress = layoutsAsArray.length() + reverseMapAsArray.length();

            notifyManager.notify(this.uid, builder.build());

            for (int i = 0; i < layoutsAsArray.length(); ++i) {
                if (Thread.interrupted()) {
                    notifyManager.cancel(this.uid);
                    return false;
                }

                JSONObject currentLayout = layoutsAsArray.getJSONObject(i);
                String name = currentLayout.getString("name");
                int ambiguity = currentLayout.getInt("ambiguity");
                LayoutIdentification layoutIdentification
                        = new LayoutIdentification(name, ambiguity, currentLayout.getJSONArray("layoutIdentifiers"));

                this.layoutIdentificationMap.put(name, layoutIdentification);
            }

            for (int i = 0; i < reverseMapAsArray.length(); ++i) {
                if (Thread.interrupted()) {
                    notifyManager.cancel(this.uid);
                    return false;
                }

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
        builder.setContentText(context.getString(R.string.notification_finished_loading_1) + " " + getPackageName()
                            + " " + context.getString(R.string.notification_finished_loading_2))
                .setProgress(0, 0, false);
        notifyManager.notify(this.uid, builder.build());

        Log.v("AppDetectionData", "Finished building hash maps from JSON");

        return true;
    }

    /**
     * Returns a set of android IDs that occur on the current screen
     * @param startNodeInfo    Starting node from which to consider objects on screen (root node for all)
     */
    private Set<String> androidIDsOnScreen(AccessibilityNodeInfo startNodeInfo) {
        Set<String> androidIDsOnScreen = new TreeSet<>(new CollatorWrapper());

        NodeInfoTraverser<String> traverser = new NodeInfoTraverser<>(startNodeInfo,
                new NodeInfoDataExtractor<String>() {
                    @Override
                    public String extractData(AccessibilityNodeInfo nodeInfo) {
                        return nodeInfo.getViewIdResourceName().replace(packageName + ":", "");
                    }
                },
                new NodeInfoFilter() {
                    @Override
                    public boolean filter(AccessibilityNodeInfo nodeInfo) {
                        return nodeInfo != null &&
                                nodeInfo.getViewIdResourceName() != null &&
                                nodeInfo.isVisibleToUser();
                    }
                });
        androidIDsOnScreen.addAll(traverser.getAllFiltered());

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
