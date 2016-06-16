package de.uni_bonn.detectappscreen;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
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
 * Created by simon on 20/01/16.
 */
public class DetectAppScreenAccessibilityService extends AccessibilityService {

    protected static final int TIME_BETWEEN_LAYOUT_COMPARISONS = 500;

    protected List<Pair<String, JSONObject>> screenDefinitions = null;
    protected long timeOfLastLayoutComparison = 0;

    protected Map<String, LayoutIdentification> layoutIdentificationMap;
    protected Map<String, Set<String>> reverseMap;


    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    protected boolean shallPerformLayoutComparison(AccessibilityNodeInfo source) {
        return source != null
                && System.currentTimeMillis() - timeOfLastLayoutComparison >= TIME_BETWEEN_LAYOUT_COMPARISONS;
    }

    protected AccessibilityNodeInfo getIdContentNodeInfo(AccessibilityNodeInfo sourceNodeInfo) {
        AccessibilityNodeInfo currentNodeInfo = sourceNodeInfo;

        // Try to find id/content by traversing the tree upwards
        while (currentNodeInfo != null && currentNodeInfo.getParent() != null) {
            String resourceName = currentNodeInfo.getViewIdResourceName();
            if (resourceName != null && resourceName.equals("android:id/content")) {
                // Found it
                return currentNodeInfo;
            }
            currentNodeInfo = currentNodeInfo.getParent();
        }

        AccessibilityNodeInfo rootNodeInfo = currentNodeInfo;

        // Traverse tree downwards (breadth-first search)
        List<AccessibilityNodeInfo> nodeInfos = new LinkedList<>();
        nodeInfos.add(currentNodeInfo); // currently the root of the tree
        while (nodeInfos.size() > 0) {
            currentNodeInfo = nodeInfos.get(0);
            if (currentNodeInfo == null) {
                nodeInfos.remove(0);
                continue;
            }
            String resourceName = currentNodeInfo.getViewIdResourceName();
            if (resourceName != null && resourceName.equals("android:id/content")) {
                // Found it
                return currentNodeInfo;
            }
            else {
                // Add all child nodes
                for (int i = 0; i < currentNodeInfo.getChildCount(); ++i)
                    nodeInfos.add(currentNodeInfo.getChild(i));
            }
            nodeInfos.remove(0);
        }

        return rootNodeInfo;
    }

    protected String compareLayouts(AccessibilityNodeInfo androidIdContent) {
        if (androidIdContent.getChildCount() < 1)
            return "Unknown screen";



        return "";
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null) {

            // New activity?
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );

                ActivityInfo activityInfo = tryGetActivity(componentName);
                boolean isActivity = activityInfo != null;
                if (isActivity)
                    Log.i("CurrentActivity", componentName.flattenToShortString());
            }
//            else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED && event.getSource() != null) {
//                if (event.getSource().getText() != null)
//                    Log.i("View clicked", event.getSource().getText().toString());
//                else if (event.getSource().getViewIdResourceName() != null)
//                    Log.i("View clicked", event.getSource().getViewIdResourceName());
//            }
//            else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SELECTED && event.getSource() != null) {
//                if (event.getSource().getText() != null)
//                    Log.i("View selected", event.getSource().getText().toString());
//                else if (event.getSource().getViewIdResourceName() != null)
//                    Log.i("View selected", event.getSource().getViewIdResourceName());
//            }
//            else if (event.getEventType() != AccessibilityEvent.TYPE_VIEW_SCROLLED && event.getSource() != null) {
//                if (event.getSource().getText() != null)
//                    Log.i("Type", event.getEventType() + ", Text: " + event.getSource().getText().toString());
//                else if (event.getSource().getViewIdResourceName() != null)
//                    Log.i("Type", event.getEventType() + ", ViewIdResName: " + event.getSource().getViewIdResourceName());
//            }

            AccessibilityNodeInfo sourceNodeInfo = event.getSource();
            if (shallPerformLayoutComparison(sourceNodeInfo)) {
                AccessibilityNodeInfo currentNodeInfo = getIdContentNodeInfo(sourceNodeInfo);

                if (event.getPackageName().toString().equalsIgnoreCase("com.whatsapp")) {
                    Set<String> androidIDsOnScreen = new TreeSet<>(Collator.getInstance());
                    Set<String> recognizedLayouts = new TreeSet<>(Collator.getInstance());
                    Set<String> possibleLayouts = new TreeSet<>(Collator.getInstance());

                    // Traverse tree downwards (breadth-first search)
                    List<AccessibilityNodeInfo> nodeInfos = new LinkedList<>();
                    nodeInfos.add(currentNodeInfo); // currently the root of the tree
                    while (nodeInfos.size() > 0) {
                        currentNodeInfo = nodeInfos.get(0);
                        if (currentNodeInfo != null) {
                            String viewIdResName = currentNodeInfo.getViewIdResourceName();
                            String currentAndroidID = viewIdResName != null ? viewIdResName.replace("com.whatsapp:", "") : "";
                            if (!currentAndroidID.equals("")) {
                                androidIDsOnScreen.add(currentAndroidID);
                            }

                            // add children
                            for (int i = 0; i < currentNodeInfo.getChildCount(); ++i)
                                nodeInfos.add(currentNodeInfo.getChild(i));
                        }


                        nodeInfos.remove(0);
                    }

                    for (String androidIDOnScreen : androidIDsOnScreen) {
                        Set<String> currentPossibleLayouts = this.reverseMap.get(androidIDOnScreen);
                        if (currentPossibleLayouts != null)
                            possibleLayouts.addAll(currentPossibleLayouts);
                    }

//                    StringBuilder sb = new StringBuilder();
//                    for (String st : this.reverseMap.keySet())
//                        sb.append(st + " ");
//
//                    Log.i("ReverseMap KeySet", sb.toString());
//                    Log.i("AndroidIDs on Screen", androidIDsOnScreen.toString());
//                    Log.i("WDebug", "POSSIBLE LAYOUTS");
                    for (String possibleLayout : possibleLayouts) {
//                        Log.i("WDebug", "possible Layout: " + possibleLayout);
                        LayoutIdentification layout = this.layoutIdentificationMap.get(possibleLayout);
                        for (Set<String> layoutIdentifierSet : layout.getLayoutIdentifiers()) {
                            if (androidIDsOnScreen.containsAll(layoutIdentifierSet)) {
                                recognizedLayouts.add(layout.getName());
                                break;
                            }
                        }
                    }

                    Log.i("Recognized Layouts: ", recognizedLayouts.toString());
                }

                timeOfLastLayoutComparison = System.currentTimeMillis();
            }


        }
//        Log.i("AccessibilityEvent", "" + event.getEventType());
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //Configure these here for compatibility with API 13 and below.
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
//        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
//        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        config.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        config.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        //config.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;

//        if (Build.VERSION.SDK_INT >= 16)
//            //Just in case this helps
//            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);

        Log.i("WDebug", "Reading layouts file...");
        JSONObject layoutsAsJSON = readJSONFile("layouts.json");
        Log.i("WDebug", "Reading reverse map file...");
        JSONObject reverseMapAsJSON = readJSONFile("reverseMap.json");

        Log.i("WDebug", "Building hashmaps...");
        this.layoutIdentificationMap = new HashMap<>();
        this.reverseMap = new HashMap<>();

        try {
            JSONArray layoutsAsArray = layoutsAsJSON.getJSONArray("layoutDefinitions");
            for (int i = 0; i < layoutsAsArray.length(); ++i) {
                JSONObject currentLayout = layoutsAsArray.getJSONObject(i);
                String name = currentLayout.getString("name");
                int ambiguity = currentLayout.getInt("ambiguity");
                LayoutIdentification layoutIdentification
                        = new LayoutIdentification(name, ambiguity, currentLayout.getJSONArray("layoutIdentifiers"));

                this.layoutIdentificationMap.put(name, layoutIdentification);
            }

            JSONArray reverseMapAsArray = reverseMapAsJSON.getJSONArray("androidIDMap");
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

        // debug test
        //Log.i("WDebug", "layouts[abc_action_bar_title_item] = " + layoutIdentificationMap.get("abc_action_bar_title_item").getLayoutIdentifiers().toString());
    }

    public JSONObject readJSONFile(String filename) {
        JSONObject result = null;

        File file = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), filename);
        try (InputStream is = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            String line = "";
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

    @Override
    public void onInterrupt() {

    }
}
