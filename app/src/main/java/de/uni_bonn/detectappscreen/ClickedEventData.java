package de.uni_bonn.detectappscreen;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Data gathered when a TYPE_VIEW_CLICKED event occurs
 */
public class ClickedEventData {
    public String androidID;
    public String text;
    public String className;

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
}
