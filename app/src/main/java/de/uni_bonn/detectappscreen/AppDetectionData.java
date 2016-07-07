package de.uni_bonn.detectappscreen;

import android.content.Context;
import android.os.Environment;
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

/**
 * Data needed for detecting layouts in an associated app. Layouts are identified by certain (if possible unique)
 * identifiers their XML files contain. For now, only android:id properties are considered. There can be only one
 * android ID that uniquely identifies a layout, or a set of several android IDs, though as few as possible are
 * favored.
 */
public class AppDetectionData {

    /** Indicates whether the data has finished loading or not */
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
    public void load() {
        this.finishedLoading = buildHashMaps(this.layoutsToLoad, this.reverseMapToLoad);
    }

    /**
     * Performs necessary operations to detect the layouts currently being used by the according app
     * @param event       Accessibility event that was triggered
     * @param activity    Current activity to add to the AppUsageDataEntry
     */
    public void checkLayout(AccessibilityEvent event, String activity) {
        if (!isFinishedLoading())
            return;

        AccessibilityNodeInfo sourceNodeInfo = event.getSource();

        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo(sourceNodeInfo);
        Set<String> androidIDsOnScreen = androidIDsOnScreen(rootNodeInfo);
        Set<String> possibleLayouts = possibleLayouts(androidIDsOnScreen);
        Set<String> recognizedLayouts = recognizedLayouts(androidIDsOnScreen, possibleLayouts);

        boolean shallLog = currentAppUsageData.addDataEntry(activity, recognizedLayouts);
        if (shallLog)
            Log.i("Recognized Layouts", recognizedLayouts.toString());
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
     * @param rootNodeInfo    Root of the node info tree that contains all objects on the current screen
     */
    private Set<String> androidIDsOnScreen(AccessibilityNodeInfo rootNodeInfo) {
        AccessibilityNodeInfo currentNodeInfo = rootNodeInfo;
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
