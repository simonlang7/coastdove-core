package de.uni_bonn.detectappscreen;

import android.accessibilityservice.AccessibilityService;
import android.os.Environment;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by simon on 20/01/16.
 */
public class DetectAppScreenAccessibilityService extends AccessibilityService {

    protected String compareScreenWithJSONObject(AccessibilityNodeInfo androidIdContent) {
        if (androidIdContent.getChildCount() < 1)
            return "Unknown screen";

        JSONObject jsonObject = InfoActivity.getJSONObject();
        AccessibilityNodeInfo screenRoot = androidIdContent.getChild(0);

        // Breadth-first search
        Queue<JSONObject> jsonQueue = new LinkedList<>();
        Queue<AccessibilityNodeInfo> nodeInfoQueue = new LinkedList<>();
        jsonQueue.add(jsonObject);
        nodeInfoQueue.add(screenRoot);

        while (!nodeInfoQueue.isEmpty()) {
            JSONObject currentJSONObject = jsonQueue.remove();
            AccessibilityNodeInfo currentNodeInfo = nodeInfoQueue.remove();

            try {
                JSONArray children = currentJSONObject.getJSONArray("children");
                if (children.length() != currentNodeInfo.getChildCount())
                    return "Unknown screen (children)";


                for (int i = 0; i < children.length(); ++i) {
                    // compare
                }
            } catch (JSONException e) {
                Log.e("WDebug", "Error traversing the nodeInfo tree: " + e.getMessage());
            }
        }

        return "Unknown screen";
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null) {
            if (event.getPackageName().toString().equalsIgnoreCase("com.whatsapp")) {
                if (event.getSource() != null) {
                    AccessibilityNodeInfo rootNodeInfo = event.getSource();
                    AccessibilityNodeInfo androidIdContent = null;
                    while (rootNodeInfo.getParent() != null) {
                        if (rootNodeInfo.getViewIdResourceName() != null &&
                            rootNodeInfo.getViewIdResourceName().equals("android:id/content")) {
                            Log.i("WDebug", "got id/content.");
                            androidIdContent = rootNodeInfo;
                            break;
                        }
                        rootNodeInfo = rootNodeInfo.getParent();
                    }
                    Log.i("WDebug", "got root node.");

                    if (androidIdContent == null) {
                        List<AccessibilityNodeInfo> nodeInfos = new LinkedList<>();
                        nodeInfos.add(rootNodeInfo);
                        while (androidIdContent == null && nodeInfos.size() > 0) {
                            AccessibilityNodeInfo currentNodeInfo = nodeInfos.get(0);
                            if (currentNodeInfo.getViewIdResourceName() != null
                                    && currentNodeInfo.getViewIdResourceName().equals("android:id/content")) {
                                androidIdContent = currentNodeInfo;
                            } else {
                                for (int i = 0; i < currentNodeInfo.getChildCount(); ++i)
                                    nodeInfos.add(currentNodeInfo.getChild(i));
                            }
                            nodeInfos.remove(0);
                        }
                    }

                    if (androidIdContent != null) {
                        compareScreenWithJSONObject(androidIdContent);


//                        Log.i("WDebug", "...finally got id/content.");
//                        if (androidIdContent.getChildCount() > 0) {
//                            if (androidIdContent.getChild(0).getChildCount() == 1)
//                                Log.i("WDebug", "On super2 awesome Mainscreen!");
//                            else if (androidIdContent.getChild(0).getChildCount() == 2)
//                                Log.i("WDebug", "On super2 awesome Conversation screen!");
//                            else
//                                Log.i("WDebug", "On unknown screen");
//                        } else
//                            Log.i("WDebug", "On another unknown screen");


                        //event.getSource().getParent().
                        //Log.i("WDebug", )
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {

    }
}
