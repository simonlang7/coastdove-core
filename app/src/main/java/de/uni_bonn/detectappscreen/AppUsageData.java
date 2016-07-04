package de.uni_bonn.detectappscreen;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Slang on 01.07.2016.
 */
public class AppUsageData {
    private class AppUsageDataEntry {
        public Date timestamp;
        public String activity;
        public Set<String> detectedLayouts;

        public AppUsageDataEntry(Date timestamp, String activity, Set<String> detectedLayouts) {
            this.timestamp = timestamp;
            this.activity = activity;
            this.detectedLayouts = detectedLayouts;
        }

        public JSONObject toJSON() {
            JSONObject result = new JSONObject();
            try {
                result.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(this.timestamp));
                result.put("activity", this.activity);
                result.put("detectedLayouts", new JSONArray(this.detectedLayouts));
            } catch (JSONException e) {
                Log.e("AppUsageDataEntry", "Unable to create JSONObject: " + e.getMessage());
            }

            return result;
        }
    }

    private String packageName;
    private List<AppUsageDataEntry> dataEntries;

    public AppUsageData(String packageName) {
        this.packageName = packageName;
        this.dataEntries = new LinkedList<>();
    }

    public void addDataEntry(Date timestamp, String activity, Set<String> detectedLayouts) {
        AppUsageDataEntry entry = new AppUsageDataEntry(timestamp, activity, detectedLayouts);
        this.dataEntries.add(entry);
    }

    public void addDataEntry(String activity, Set<String> detectedLayouts) {
        Date timestamp = new Date();
        addDataEntry(timestamp, activity, detectedLayouts);
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        try {
            result.put("_type", "AppUsageData");
            result.put("package", this.packageName);

            JSONArray dataEntriesJSON = new JSONArray();
            for (AppUsageDataEntry entry : dataEntries)
                dataEntriesJSON.put(entry.toJSON());

            result.put("dataEntries", dataEntriesJSON);
        } catch (JSONException e) {
            Log.e("AppUsageData", "Unable to create JSONObject for " + this.packageName + ": " + e.getMessage());
        }

        return result;
    }

    public String getFilename() {
        if (this.dataEntries.size() == 0)
            return "empty.json";
        Date first = this.dataEntries.get(0).timestamp;
        String format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(first);
        return format + ".json";
    }

    public String getPackageName() {
        return packageName;
    }
}
