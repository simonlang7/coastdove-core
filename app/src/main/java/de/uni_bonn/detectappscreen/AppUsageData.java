package de.uni_bonn.detectappscreen;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Data collected from app usage, typically contains a list of timestamps associated
 * with an activity and a list of layouts detected at that time
 */
public class AppUsageData {
    /** Name of the package associated with these app usage data */
    private String packageName;
    /** Data entries, each containing detailed information */
    private LinkedList<AppUsageDataEntry> dataEntries;

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

                AppUsageDataEntry dataEntry = null;
                if (dataEntryJSON.has("detectedLayouts"))
                    dataEntry = new LayoutDataEntry(dataEntryJSON);
                else if (dataEntryJSON.has("detectedClick"))
                    dataEntry = new ClickDataEntry(dataEntryJSON);

                if (dataEntry != null)
                    this.dataEntries.add(dataEntry);
            }
        } catch (JSONException e) {
            Log.e("AppUsageData", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    /**
     * Adds a layout data entry
     * @param timestamp          Time at which the data were collected
     * @param activity           Activity detected
     * @param detectedLayouts    Layouts detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     */
    public boolean addLayoutDataEntry(Date timestamp, String activity, Set<String> detectedLayouts) {
        boolean lastEntryEqual = increasePreviousEntryCountIfEqual(new LayoutDataEntry(null, activity, detectedLayouts));
        if (!lastEntryEqual) {
            AppUsageDataEntry entry = new LayoutDataEntry(timestamp, activity, detectedLayouts);
            this.dataEntries.add(entry);
            return true;
        }
        else
            return false;
    }

    /**
     * Adds a layout data entry, using the current time when creating the timestamp
     * @param activity           Activity detected
     * @param detectedLayouts    Layouts detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     */
    public boolean addLayoutDataEntry(String activity, Set<String> detectedLayouts) {
        Date timestamp = new Date();
        return addLayoutDataEntry(timestamp, activity, detectedLayouts);
    }

    /**
     * Adds a click data entry
     * @param timestamp        Time at which the data were collected
     * @param activity         Activity detected
     * @param detectedClick    Click detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     */
    public boolean addClickDataEntry(Date timestamp, String activity, Set<ClickedEventData> detectedClick) {
        boolean lastEntryEqual = increasePreviousEntryCountIfEqual(new ClickDataEntry(null, activity, detectedClick));
        if (!lastEntryEqual) {
            AppUsageDataEntry entry = new ClickDataEntry(timestamp, activity, detectedClick);
            this.dataEntries.add(entry);
            return true;
        }
        else
            return false;
    }

    /**
     * Adds a click data entry, using the current time when creating the timestamp
     * @param activity         Activity detected
     * @param detectedClick    Click detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     */
    public boolean addClickDataEntry(String activity, Set<ClickedEventData> detectedClick) {
        Date timestamp = new Date();
        return addClickDataEntry(timestamp, activity, detectedClick);
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
        Date first = this.dataEntries.get(0).getTimestamp();
        String format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(first);
        return format + ".json";
    }

    /**
     * Returns the name of the package associated with these data
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * If the previous entry of the same type is equal to the given one, that entry's
     * count is increased.
     * @param other    Entry to compare the previous same-type entry with
     * @return True if the entries are equal, false otherwise
     */
    private boolean increasePreviousEntryCountIfEqual(AppUsageDataEntry other) {
        AppUsageDataEntry previousEntry = null;
        if (other instanceof ClickDataEntry) {
            AppUsageDataEntry last = this.dataEntries.peekLast();
            if (last != null && last instanceof ClickDataEntry) {
                previousEntry = last;
            }
        }
        else {
            Iterator<AppUsageDataEntry> it = this.dataEntries.descendingIterator();
            while (it.hasNext()) {
                AppUsageDataEntry entry = it.next();
                if (entry instanceof LayoutDataEntry) {
                    previousEntry = entry;
                    break;
                }
            }
        }

        if (previousEntry != null && previousEntry.equals(other)) {
            previousEntry.increaseCount();
            return true;
        }
        else
            return false;
    }
}
