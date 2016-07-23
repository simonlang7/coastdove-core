package de.uni_bonn.detectappscreen.app_usage;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data gathered when a TYPE_VIEW_CLICKED event occurs
 */
public class ClickedEventData {
    private String androidID;
    private String text;
    private String className;

    public ClickedEventData(JSONObject dataJSON) {
        this.androidID = null;
        this.text = null;
        this.className = null;
        try {
            this.androidID = dataJSON.getString("androidID");
            this.text = dataJSON.getString("text");
            this.className = dataJSON.getString("className");
        } catch (JSONException e) {
            Log.e("ClickedEventData", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    public ClickedEventData(String androidID, String text, String className) {
        this.androidID = androidID;
        this.text = text;
        this.className = className;
    }

    public ClickedEventData(AccessibilityNodeInfo nodeInfo) {
        this.androidID = nodeInfo.getViewIdResourceName() != null ? nodeInfo.getViewIdResourceName() : "";
        this.text = nodeInfo.getText() != null ? nodeInfo.getText().toString().replaceAll("\n", " ") : "";
        this.className = nodeInfo.getClassName() != null ? nodeInfo.getClassName().toString() : "";
    }

    @Override
    public String toString() {
        return "(ID: " + androidID + ", Text: " + text + ", Class: " + className + ")";
    }

    public boolean equals(ClickedEventData other) {
        return this.androidID.equals(other.androidID) &&
                this.text.equals(other.text) &&
                this.className.equals(other.className);
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();

        try {
            result.put("androidID", this.androidID);
            result.put("text", this.text);
            result.put("className", this.className);
        } catch (JSONException e) {
            Log.e("ClickedEventData", "Unable to create JSONObject for " + androidID + " (" + text + "): " + e.getMessage());
        }

        return result;
    }

    public String getClassName() {
        return className;
    }

    public String getText() {
        return text;
    }

    public String getAndroidID() {
        return androidID;
    }
}
