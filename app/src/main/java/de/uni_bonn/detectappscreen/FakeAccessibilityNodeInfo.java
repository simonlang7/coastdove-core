package de.uni_bonn.detectappscreen;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Slang on 10.03.2016.
 */
public class FakeAccessibilityNodeInfo {
    protected String viewIdResourceName;
    protected String text;
    protected String contentDescription;
    protected String className;

    protected ArrayList<FakeAccessibilityNodeInfo> children;
    protected FakeAccessibilityNodeInfo parent;


    public FakeAccessibilityNodeInfo(JSONObject jsonObject) {
        try {
            this.viewIdResourceName = jsonObject.getString("viewIdResName");
            this.text = jsonObject.getString("text");
            this.contentDescription = jsonObject.getString("contentDescription");
            this.className = jsonObject.getString("className");
        } catch (JSONException e) {
            Log.e("DetectAppScreen", "Unable to create node: " + e.getMessage());
        }
    }


    public void addChild(FakeAccessibilityNodeInfo child) {
        this.children.add(child);
    }

    public FakeAccessibilityNodeInfo getChild(int index) {
        return children.get(index);
    }

    public int getChildCount() {
        return children.size();
    }

    public FakeAccessibilityNodeInfo getParent() {
        return parent;
    }

    public void setParent(FakeAccessibilityNodeInfo parent) {
        this.parent = parent;
    }

    public String getViewIdResourceName() {
        return viewIdResourceName;
    }

    public void setViewIdResourceName(String viewIdResourceName) {
        this.viewIdResourceName = viewIdResourceName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getContentDescription() {
        return contentDescription;
    }

    public void setContentDescription(String contentDescription) {
        this.contentDescription = contentDescription;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
