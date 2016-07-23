package de.uni_bonn.detectappscreen.analyze;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Contains meta information regarding a detectable app, such as entry activities
 */
public class AppMetaInformation {
    /** Package name of the app */
    private String appPackageName;
    /** Activities that are entry points to the app from a launcher */
    private List<String> mainActivities;

    /**
     * Creates AppMetaInformation with the given data
     */
    public AppMetaInformation(String appPackageName, List<String> mainActivities) {
        this.appPackageName = appPackageName;
        this.mainActivities = mainActivities;
    }

    /**
     * Creates AppMetaInformation from a JSONObject
     */
    public AppMetaInformation(JSONObject appMetaInformationJSON) {
        this.mainActivities = new LinkedList<>();
        try {
            JSONArray mainActivitiesJSON = appMetaInformationJSON.getJSONArray("mainActivities");
            for (int i = 0; i < mainActivitiesJSON.length(); ++i)
                this.mainActivities.add(mainActivitiesJSON.getString(i));
        } catch (JSONException e) {
            Log.e("AppMetaInformation", "Error reading from JSONObject: " + e.getMessage());
        }
    }

    public boolean isMainActivity(String activity) {
        for (String mainActivity : this.mainActivities) {
            if (mainActivity.replaceAll("/", "").replaceAll("\\", "").contains(activity))
                return true;
        }
        return false;
    }

    /** Package name of the app */
    public String getAppPackageName() {
        return appPackageName;
    }

    /** Activities that are entry points to the app from a launcher */
    public List<String> getMainActivities() {
        return mainActivities;
    }
}
