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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.app_usage.ActivityDataEntry;
import de.uni_bonn.detectappscreen.app_usage.AppUsageData;
import de.uni_bonn.detectappscreen.app_usage.AppUsageDataProcessor;
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
public class AppDetectionData implements Serializable {
    private static final long serialVersionUID = 3582683828213329450L;


    /** Name of the package associated, i.e. the app that can be detected */
    private String appPackageName;
    /** Hash map mapping from each layout to a set of android IDs that identify the layout*/
    private Map<String, LayoutIdentification> layoutIdentificationMap;
    /** Reverse hash map, mapping from sets of android IDs to layouts that can possibly be identified */
    private Map<String, Set<String>> reverseMap;
    /** Contains information about main activities */
    private AppMetaInformation appMetaInformation;

    /** Indicates whether to perform layout checks or not */
    private transient boolean performLayoutChecks;
    /** Indicates whether to perform interaction checks or not */
    private transient boolean performInteractionChecks;

    /** Usage data collected for this session (that starts when the app is opened and ends when it's closed) */
    private transient AppUsageData currentAppUsageData;

    /** Application context */
    private transient Context context;

    /**
     * Creates an AppDetectionData object using the given parameters
     * @param appPackageName             Package name of the detectable app
     * @param layoutIdentificationMap    Map (layout -> {android IDs})
     * @param reverseMap                 Map (androidID -> {possible layouts})
     */
    public AppDetectionData(String appPackageName, Map<String, LayoutIdentification> layoutIdentificationMap,
                            Map<String, Set<String>> reverseMap, AppMetaInformation appMetaInformation) {
        this.appPackageName = appPackageName;
        this.layoutIdentificationMap = layoutIdentificationMap;
        this.reverseMap = reverseMap;
        this.appMetaInformation = appMetaInformation;
    }

    /**
     * Initialized the AppDetectionData - only call this if the object was de-serialized
     * @param performLayoutChecks         Whether to perform layouts checks
     * @param performInteractionChecks    Whether to perform interaction checks
     * @param context                     App context
     */
    public void init(boolean performLayoutChecks, boolean performInteractionChecks, Context context) {
        this.performLayoutChecks = performLayoutChecks;
        this.performInteractionChecks = performInteractionChecks;
        this.currentAppUsageData = null;
        this.context = context;
    }

    /**
     * Performs necessary operations to detect the layouts currently being used by the according app,
     * and/or gesture events and/or interaction events
     * @param event       Accessibility event that was triggered
     * @param activity    Current activity to add to the ActivityDataEntry
     */
    public void performChecks(AccessibilityEvent event, AccessibilityNodeInfo rootNodeInfo, String activity) {
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
        this.currentAppUsageData = null;
    }

    /**
     * Creates a new app usage data
     */
    private void newAppUsageData() {
        currentAppUsageData = new AppUsageData(this.appPackageName);
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
                                        !nodeInfo.getViewIdResourceName().endsWith("id/list"));
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
        AppUsageDataProcessor processor = new AppUsageDataProcessor(new AppMetaInformation(this.appPackageName, new LinkedList<String>()), appUsageData);
        processor.process();
        new Thread(new SQLiteWriter(this.context, appUsageData)).start();
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

    /**
     * Returns the package name of the app to be detected by these data
     */
    public String getAppPackageName() {
        return appPackageName;
    }

    /** Contains information about main activities */
    public AppMetaInformation getAppMetaInformation() {
        return appMetaInformation;
    }

    public void setPerformLayoutChecks(boolean performLayoutChecks) {
        this.performLayoutChecks = performLayoutChecks;
    }

    public void setPerformInteractionChecks(boolean performInteractionChecks) {
        this.performInteractionChecks = performInteractionChecks;
    }

    public boolean getPerformLayoutChecks() {
        return this.performLayoutChecks;
    }

    public boolean getPerformInteractionChecks() {
        return this.performInteractionChecks;
    }
}
