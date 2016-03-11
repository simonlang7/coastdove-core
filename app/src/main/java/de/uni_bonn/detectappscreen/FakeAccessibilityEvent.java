package de.uni_bonn.detectappscreen;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Slang on 10.03.2016.
 */
public class FakeAccessibilityEvent {

    protected int versionMajor;
    protected int versionMinor;
    protected String packageName;
    protected String className;


    public FakeAccessibilityEvent(JSONObject jsonObject) {
        try {
            versionMajor = jsonObject.getInt("versionMajor");
            versionMinor = jsonObject.getInt("versionMinor");
            packageName = jsonObject.getString("packageName");
            className = jsonObject.getString("className");
        } catch (JSONException e) {
            Log.e("DetectAppScreen", "Error processing FakeAccessibilityEvent: " + e.getMessage());
        }
    }



    public int getVersionMajor() {
        return versionMajor;
    }

    public int getVersionMinor() {
        return versionMinor;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }
}
