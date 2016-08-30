/*  DetectAppScreen
    Copyright (C) 2016  Simon Lang
    Contact: simon.lang7 at gmail dot com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.uni_bonn.detectappscreen.detection;

import android.content.Context;
import android.graphics.Rect;
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

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.app_usage.ActivityDataEntry;
import de.uni_bonn.detectappscreen.app_usage.AppUsageData;
import de.uni_bonn.detectappscreen.app_usage.AppUsageDataWriter;
import de.uni_bonn.detectappscreen.app_usage.InteractionEventData;
import de.uni_bonn.detectappscreen.app_usage.sql.SQLiteWriter;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.utility.CollatorWrapper;
import de.uni_bonn.detectappscreen.utility.FileHelper;

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

    /** UI elements to display loading progress */
    private LoadingInfo hashMapLoadingInfo;

    /** Name of the package associated, i.e. the app that can be detected */
    private String appPackageName;
    /** Indicates whether the hash maps (layoutIdentificationMap and reverseMap) have been loaded or not */
    private boolean hashMapsLoaded;
    /** Hash map mapping from each layout to a set of android IDs that identify the layout*/
    private Map<String, LayoutIdentification> layoutIdentificationMap;
    /** Reverse hash map, mapping from sets of android IDs to layouts that can possibly be identified */
    private Map<String, Set<String>> reverseMap;

    /** Indicates whether to perform layout checks or not */
    private boolean performLayoutChecks;
    /** Indicates whether to perform interaction checks or not */
    private boolean performInteractionChecks;

    /** Usage data collected for this session (that starts when the app is opened and ends when it's closed) */
    private AppUsageData currentAppUsageData;

    /** Application context */
    private Context context;

    /**
     * Creates an AppDetectionData object using the given parameters
     * @param appPackageName    Name of the package for app detection
     * @param layouts           Layouts in JSON format
     * @param reverseMap        Reverse map in JSON format
     */
    public AppDetectionData(String appPackageName, JSONObject layouts, JSONObject reverseMap, Context context) {
        this.finishedLoading = false;
        this.performLayoutChecks = false;
        this.performInteractionChecks = false;
        this.appPackageName = appPackageName;
        this.hashMapsLoaded = false;
        this.layoutsToLoad = layouts;
        this.reverseMapToLoad = reverseMap;
        this.layoutIdentificationMap = null;
        this.reverseMap = null;
        this.currentAppUsageData = null;
        this.context = context;
        this.hashMapLoadingInfo = new LoadingInfo();
    }

    /**
     * Creates an AppDetectionData object using the given parameters, automatically loads
     * the layouts and reverse map files from the according directory
     * @param appPackageName    Name of the package for app detection
     */
    public AppDetectionData(String appPackageName, Context context) {
        this.finishedLoading = false;
        this.performLayoutChecks = false;
        this.performInteractionChecks = false;
        this.appPackageName = appPackageName;
        this.hashMapsLoaded = false;
        this.layoutsToLoad = FileHelper.readJSONFile(
                context, FileHelper.Directory.PACKAGE, appPackageName, "layouts.json");
        this.reverseMapToLoad = FileHelper.readJSONFile(
                context, FileHelper.Directory.PACKAGE, appPackageName, "reverseMap.json");
        this.layoutIdentificationMap = null;
        this.reverseMap = null;
        this.currentAppUsageData = null;
        this.context = context;
        this.hashMapLoadingInfo = new LoadingInfo();
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
    public void load(boolean performLayoutChecks, boolean performInteractionChecks) {
        this.performLayoutChecks = performLayoutChecks;
        this.performInteractionChecks = performInteractionChecks;
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
                if (this.hashMapsLoaded && !FileHelper.fileExists(this.context, FileHelper.Directory.PACKAGE, getAppPackageName(), "layouts.bin"))
                    FileHelper.writeHashMap(this.context, (HashMap)this.layoutIdentificationMap, FileHelper.Directory.PACKAGE, getAppPackageName(), "layouts.bin");
                if (this.hashMapsLoaded && !FileHelper.fileExists(this.context, FileHelper.Directory.PACKAGE, getAppPackageName(), "reverseMap.bin"))
                    FileHelper.writeHashMap(this.context, (HashMap)this.reverseMap, FileHelper.Directory.PACKAGE, getAppPackageName(), "reverseMap.bin");
            }

            finishedLoading = hashMapsLoaded;
        }

        this.finishedLoading = finishedLoading;
    }

    /**
     * Performs necessary operations to detect the layouts currently being used by the according app,
     * and/or gesture events and/or interaction events
     * @param event       Accessibility event that was triggered
     * @param activity    Current activity to add to the ActivityDataEntry
     */
    public void performChecks(AccessibilityEvent event, AccessibilityNodeInfo rootNodeInfo, String activity) {
        if (!isFinishedLoading())
            return;

        if (this.currentAppUsageData == null)
            newAppUsageData();

        if (shallPerformActivityChecks(event)) {
            boolean shallLog = currentAppUsageData.addActivityData(activity);
            if (shallLog)
                Log.i("Activity", activity);
        }

        if (shallPerformLayoutChecks(event)) {
            Set<String> recognizedLayouts = checkLayouts(event.getSource(), rootNodeInfo);

            boolean shallLog = currentAppUsageData.addLayoutDataEntry(activity, recognizedLayouts);
            if (shallLog)
                Log.i("Recognized layouts", recognizedLayouts.toString());
        }

//        if (shallPerformScrollChecks(event)) {
//            String scrolledElement = checkScrollEvent(event.getSource(), rootNodeInfo);
//
//            boolean shallLog = currentAppUsageData.addScrollDataEntry(activity, scrolledElement);
//            if (shallLog)
//                Log.i("Scrolled element", scrolledElement);
//        }

        if (shallPerformInteractionChecks(event)) {
            Set<InteractionEventData> interactionEventData = checkInteractionEvents(event.getSource(), rootNodeInfo);
            ActivityDataEntry.EntryType type;
            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    type = ActivityDataEntry.EntryType.CLICK;
                    break;
                case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                    type = ActivityDataEntry.EntryType.LONG_CLICK;
                    break;
                case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                    type = ActivityDataEntry.EntryType.SCROLLING;
                    break;
                default:
                    type = ActivityDataEntry.EntryType.OTHER;
                    break;
            }

            boolean shallLog = currentAppUsageData.addInteractionDataEntry(activity, interactionEventData, type);
            if (shallLog)
                Log.i("Interaction events", interactionEventData.toString());
        }

    }

    /**
     * Writes the current app usage data to a file and swaps it for an empty new one,
     * shall be called whenever the app to be detected is exited
     */
    public void saveAppUsageData() {
        currentAppUsageData.finish();
        writeAppUsageData();
    }

    /**
     * Creates a new app usage data
     */
    private void newAppUsageData() {
        currentAppUsageData = new AppUsageData(this.appPackageName);
    }

    /**
     * Returns the package name of the app to be detected by these data
     */
    public String getAppPackageName() {
        return appPackageName;
    }

    public boolean getPerformLayoutChecks() {
        return this.performLayoutChecks;
    }

    public boolean getPerformInteractionChecks() {
        return this.performInteractionChecks;
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
     * Returns true iff an interaction event check shall be performed
     */
    private boolean shallPerformInteractionChecks(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED &&
                event.getEventType() != AccessibilityEvent.TYPE_VIEW_SCROLLED)
            return false;
        if (event.getSource() == null)
            return false;
        if (!performInteractionChecks)
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
     * Handles interaction events, i.e. creates a data entry showing which element was interacted with
     * @param source    Source node info
     * @return Data regarding this interaction event
     */
    private Set<InteractionEventData> checkInteractionEvents(AccessibilityNodeInfo source, AccessibilityNodeInfo rootNodeInfo) {
        Set<InteractionEventData> result = new CopyOnWriteArraySet<>();

        // Try to find the node info with the same bounds as the source.
        // For some reason, the source does not contain any View ID information,
        // so we need to find the same node info again in order to get the view ID.
        AccessibilityNodeInfo subTree = findNodeInfo(source, rootNodeInfo);

        if (subTree == null)
            subTree = source;

        NodeInfoTraverser<InteractionEventData> traverser = new NodeInfoTraverser<>(subTree,
                new NodeInfoDataExtractor<InteractionEventData>() {
                    @Override
                    public InteractionEventData extractData(AccessibilityNodeInfo nodeInfo) {
                        return new InteractionEventData(nodeInfo);
                    }
                },
                new NodeInfoFilter() {
                    @Override
                    public boolean filter(AccessibilityNodeInfo nodeInfo) {
                        return nodeInfo != null &&
                                (nodeInfo.getViewIdResourceName() != null || nodeInfo.getText() != null) &&
                                (nodeInfo.getViewIdResourceName() == null ||
                                        nodeInfo.getViewIdResourceName().endsWith("id/list"));
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
                result.add(new InteractionEventData(parent));
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
        new Thread(new SQLiteWriter(this.context, appUsageData)).start();
    }

    /**
     * Constructs hash maps (layout -> (layout identifiers))
     * and (android ID -> (possible layouts))
     * from serialized objects
     * @return True if building the hash maps was successful, false otherwise
     */
    private boolean buildHashMapsFromBinary() {
        // todo: constants for filenames

        hashMapLoadingInfo.setNotificationData(context.getString(R.string.app_name),
                context.getString(R.string.notification_loading_1) + " " + getAppPackageName()
                        + " " + context.getString(R.string.notification_loading_2),
                R.drawable.notification_template_icon_bg);
        hashMapLoadingInfo.start(true);


        if (FileHelper.fileExists(this.context, FileHelper.Directory.PACKAGE, getAppPackageName(), "layouts.bin") &&
                FileHelper.fileExists(this.context, FileHelper.Directory.PACKAGE, getAppPackageName(), "reverseMap.bin")) {
            if (Thread.currentThread().isInterrupted()) {
                hashMapLoadingInfo.cancel();
                return false;
            }

//            if (hashMapLoadingInfo != null)
//                hashMapLoadingInfo.update();

            Log.v("AppDetectionData", "Building hash maps from binary...");
            this.layoutIdentificationMap =
                    (Map<String, LayoutIdentification>)FileHelper.readHashMap(this.context, FileHelper.Directory.PACKAGE, getAppPackageName(), "layouts.bin");

            if (Thread.currentThread().isInterrupted()) {
                hashMapLoadingInfo.cancel();
                return false;
            }

            this.reverseMap = (Map<String, Set<String>>)FileHelper.readHashMap(this.context, FileHelper.Directory.PACKAGE, getAppPackageName(), "reverseMap.bin");
            Log.v("AppDetectionData", "Finished building hash maps from binary");

            if (Thread.currentThread().isInterrupted()) {
                hashMapLoadingInfo.cancel();
                return false;
            }

            hashMapLoadingInfo.setNotificationData(null,
                    context.getString(R.string.notification_finished_loading_1) + " " + getAppPackageName()
                            + " " + context.getString(R.string.notification_finished_loading_2),
                    null);
            hashMapLoadingInfo.end();

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


        hashMapLoadingInfo.setNotificationData(context.getString(R.string.app_name),
                context.getString(R.string.notification_loading_1) + " " + getAppPackageName()
                        + " " + context.getString(R.string.notification_loading_2),
                R.drawable.notification_template_icon_bg);
        hashMapLoadingInfo.start(true);

        try {
            JSONArray layoutsAsArray = layouts.getJSONArray("layoutDefinitions");
            JSONArray reverseMapAsArray = reverseMap.getJSONArray("androidIDMap");
            this.layoutIdentificationMap = new HashMap<>(layoutsAsArray.length(), 1.0f);
            this.reverseMap = new HashMap<>(reverseMapAsArray.length(), 1.0f);
            //int maxProgress = layoutsAsArray.length() + reverseMapAsArray.length();

            for (int i = 0; i < layoutsAsArray.length(); ++i) {
                if (Thread.interrupted()) {
                    hashMapLoadingInfo.cancel();
                    return false;
                }

                JSONObject currentLayout = layoutsAsArray.getJSONObject(i);
                String name = currentLayout.getString("name");
                int ambiguity = 1;//currentLayout.getInt("ambiguity"); // todo: Hotfix! Gonna remove method anyway
                LayoutIdentification layoutIdentification
                        = new LayoutIdentification(name, ambiguity, currentLayout.getJSONArray("layoutIdentifiers"));

                this.layoutIdentificationMap.put(name, layoutIdentification);
            }

            for (int i = 0; i < reverseMapAsArray.length(); ++i) {
                if (Thread.interrupted()) {
                    hashMapLoadingInfo.cancel();
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

        hashMapLoadingInfo.setNotificationData(null,
                context.getString(R.string.notification_finished_loading_1) + " " + getAppPackageName()
                        + " " + context.getString(R.string.notification_finished_loading_2),
                null);
        hashMapLoadingInfo.end();

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
                        return nodeInfo.getViewIdResourceName().replace(appPackageName + ":", "");
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

    public void setHashMapLoadingInfo(LoadingInfo hashMapLoadingInfo) {
        this.hashMapLoadingInfo = hashMapLoadingInfo;
    }
}
