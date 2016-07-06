package de.uni_bonn.detectappscreen;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Data collected from app usage, typically contains a list of timestamps associated
 * with an activity and a list of layouts detected at that time
 */
public class AppUsageData {
    /**
     * Contains a timestamp as well as an activity and a list of layouts detected at that time
     */
    private class AppUsageDataEntry {
        /** Time at which these data were collected */
        public Date timestamp;
        /** Activity detected */
        public String activity;
        /** Layouts detected */
        public Set<String> detectedLayouts;

        /**
         * Creates a new app usage data entry
         * @param timestamp          Time at which the data were collected
         * @param activity           Activity detected
         * @param detectedLayouts    Layouts detected
         */
        public AppUsageDataEntry(Date timestamp, String activity, Set<String> detectedLayouts) {
            this.timestamp = timestamp;
            this.activity = activity;
            this.detectedLayouts = detectedLayouts;
        }

        /**
         * Creates a new app usage data entry from a JSONObject
         * @param entryJSON An entry in JSON format
         */
        public AppUsageDataEntry(JSONObject entryJSON) {
            this.timestamp = new Date(new GregorianCalendar(0, 1, 1).getTimeInMillis());
            this.activity = "";
            this.detectedLayouts = new TreeSet<>(Collator.getInstance());
            try {
                this.activity = entryJSON.getString("activity");
                String timestamp = entryJSON.getString("timestamp");
                this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").parse(timestamp);
                JSONArray detectedLayoutsJSON = entryJSON.getJSONArray("detectedLayouts");
                for (int i = 0; i < detectedLayoutsJSON.length(); ++i) {
                    String layout = detectedLayoutsJSON.getString(i);
                    this.detectedLayouts.add(layout);
                }
            } catch (JSONException e) {
                Log.e("AppUsageDataEntry", "Unable to read from JSONObject: " + e.getMessage());
            } catch (ParseException e) {
                Log.e("AppUsageDataEntry", "Unable to parse timestamp: " + e.getMessage());
            }
        }

        /**
         * Converts the entry to JSON and returns the according JSONObject
         */
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

    /** Name of the package associated with these app usage data */
    private String packageName;
    /** Data entries, each containing detailed information */
    private List<AppUsageDataEntry> dataEntries;

    /**
     * Constructs an AppUsageData object with the given package name and an empty list
     * of data entries
     * @param packageName    Name of the app / package associated
     */
    public AppUsageData(String packageName) {
        this.packageName = packageName;
        this.dataEntries = new LinkedList<>();
    }

    /**
     * Constructs an AppUsageData object from a JSONObject and fills it with the included
     * data
     * @param dataJSON    JSONObject containing the data
     */
    public AppUsageData(JSONObject dataJSON) {
        this.packageName = "";
        this.dataEntries = new LinkedList<>();
        try {
            this.packageName = dataJSON.getString("package");
            JSONArray dataEntriesJSON = dataJSON.getJSONArray("dataEntries");
            for (int i = 0; i < dataEntriesJSON.length(); ++i) {
                JSONObject dataEntryJSON = dataEntriesJSON.getJSONObject(i);
                AppUsageDataEntry dataEntry = new AppUsageDataEntry(dataEntryJSON);
                this.dataEntries.add(dataEntry);
            }
        } catch (JSONException e) {
            Log.e("AppUsageData", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    /**
     * Adds a data entry
     * @param timestamp          Time at which the data were collected
     * @param activity           Activity detected
     * @param detectedLayouts    Layouts detected
     */
    public void addDataEntry(Date timestamp, String activity, Set<String> detectedLayouts) {
        AppUsageDataEntry entry = new AppUsageDataEntry(timestamp, activity, detectedLayouts);
        this.dataEntries.add(entry);
    }

    /**
     * Adds a data entry, using the current time when creating the timestamp
     * @param activity           Activity detected
     * @param detectedLayouts    Layouts detected
     */
    public void addDataEntry(String activity, Set<String> detectedLayouts) {
        Date timestamp = new Date();
        addDataEntry(timestamp, activity, detectedLayouts);
    }

    /**
     * Converts the AppUsageData object to JSON and returns the according JSONObject
     */
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

    /**
     * Returns the filename for saving this object
     */
    public String getFilename() {
        if (this.dataEntries.size() == 0)
            return "empty.json";
        Date first = this.dataEntries.get(0).timestamp;
        String format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(first);
        return format + ".json";
    }

    /**
     * Returns the name of the package associated with these data
     */
    public String getPackageName() {
        return packageName;
    }
}
