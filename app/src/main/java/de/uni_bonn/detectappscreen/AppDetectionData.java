package de.uni_bonn.detectappscreen;

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
 * Created by Slang on 17.06.2016.
 */
public class AppDetectionData {

    protected String packageName;
    protected Map<String, LayoutIdentification> layoutIdentificationMap;
    protected Map<String, Set<String>> reverseMap;

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

    public AppDetectionData(String packageName, JSONObject layouts, JSONObject reverseMap) {
        buildHashMaps(packageName, layouts, reverseMap);
    }

    public AppDetectionData(String packageName) {
        Log.i("WDebug", "Reading layouts file...");
        JSONObject layoutsAsJSON = AppDetectionData.readJSONFile(packageName, "layouts.json");
        Log.i("WDebug", "Reading reverse map file...");
        JSONObject reverseMapAsJSON = AppDetectionData.readJSONFile(packageName, "reverseMap.json");

        buildHashMaps(packageName, layoutsAsJSON, reverseMapAsJSON);
    }

    private void buildHashMaps(String packageName, JSONObject layouts, JSONObject reverseMap) {
        this.packageName = packageName;
        Log.i("WDebug", "Building hashmaps...");
        this.layoutIdentificationMap = new HashMap<>();
        this.reverseMap = new HashMap<>();

        try {
            JSONArray layoutsAsArray = layouts.getJSONArray("layoutDefinitions");
            for (int i = 0; i < layoutsAsArray.length(); ++i) {
                JSONObject currentLayout = layoutsAsArray.getJSONObject(i);
                String name = currentLayout.getString("name");
                int ambiguity = currentLayout.getInt("ambiguity");
                LayoutIdentification layoutIdentification
                        = new LayoutIdentification(name, ambiguity, currentLayout.getJSONArray("layoutIdentifiers"));

                this.layoutIdentificationMap.put(name, layoutIdentification);
            }

            JSONArray reverseMapAsArray = reverseMap.getJSONArray("androidIDMap");
            for (int i = 0; i < reverseMapAsArray.length(); ++i) {
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
    }

    protected AccessibilityNodeInfo getRootNodeInfo(AccessibilityNodeInfo sourceNodeInfo) {
        AccessibilityNodeInfo currentNodeInfo = sourceNodeInfo;

        while (currentNodeInfo != null && currentNodeInfo.getParent() != null)
            currentNodeInfo = currentNodeInfo.getParent();

        return currentNodeInfo;
    }

    protected Set<String> androidIDsOnScreen(AccessibilityNodeInfo rootNodeInfo) {
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

                // add children
                for (int i = 0; i < currentNodeInfo.getChildCount(); ++i)
                    nodeInfos.add(currentNodeInfo.getChild(i));
            }


            nodeInfos.remove(0);
        }

        return androidIDsOnScreen;
    }

    protected Set<String> possibleLayouts(Set<String> androidIDs) {
        Set<String> possibleLayouts = new TreeSet<>(Collator.getInstance());
        for (String androidIDOnScreen : androidIDs) {
            Set<String> currentPossibleLayouts = this.reverseMap.get(androidIDOnScreen);
            if (currentPossibleLayouts != null)
                possibleLayouts.addAll(currentPossibleLayouts);
        }

        return possibleLayouts;
    }

    protected Set<String> recognizedLayouts(Set<String> androidIDs, Set<String> possibleLayouts) {
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

    public void checkLayout(AccessibilityEvent event) {
        AccessibilityNodeInfo sourceNodeInfo = event.getSource();

        AccessibilityNodeInfo rootNodeInfo = getRootNodeInfo(sourceNodeInfo);
        Set<String> androidIDsOnScreen = androidIDsOnScreen(rootNodeInfo);
        Set<String> possibleLayouts = possibleLayouts(androidIDsOnScreen);
        Set<String> recognizedLayouts = recognizedLayouts(androidIDsOnScreen, possibleLayouts);

        Log.i("Recognized Layouts: ", recognizedLayouts.toString());

    }

    public String getPackageName() {
        return packageName;
    }
}
