/*  DetectAppScreen
    Copyright (C) 2016  Simon Lang
    Contact: simon.lang7 at gmail dot com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.uni_bonn.detectappscreen.app_usage;

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

                AppUsageDataEntry dataEntry;
                if (dataEntryJSON.has("detectedLayouts"))
                    dataEntry = new LayoutDataEntry(dataEntryJSON);
                else if (dataEntryJSON.has("detectedClick"))
                    dataEntry = new ClickDataEntry(dataEntryJSON);
                else if (dataEntryJSON.has("scrolledElement"))
                    dataEntry = new ScrollDataEntry(dataEntryJSON);
                else
                    dataEntry = new ActivityDataEntry(dataEntryJSON);

                this.dataEntries.add(dataEntry);
            }
        } catch (JSONException e) {
            Log.e("AppUsageData", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    /**
     * Adds an activity data entry
     * @param timestamp    Time at which the data were collected
     * @param activity     Activity detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     */
    public boolean addActivityDataEntry(Date timestamp, String activity) {
        boolean lastEntryEqual = increasePreviousEntryCountIfEqual(new ActivityDataEntry(null, activity));
        if (!lastEntryEqual) {
            AppUsageDataEntry entry = new ActivityDataEntry(timestamp, activity);
            this.dataEntries.add(entry);
            return true;
        }
        else
            return false;
    }

    /**
     * Adds an activity data entry, using the current time when creating the timestamp
     * @param activity           Activity detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     */
    public boolean addActivityDataEntry(String activity) {
        Date timestamp = new Date();
        return addActivityDataEntry(timestamp, activity);
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
     * Adds a scroll data entry
     * @param timestamp        Time at which the data were collected
     * @param activity         Activity detected
     * @return True if there was a previous scroll data entry, false otherwise
     */
    public boolean addScrollDataEntry(Date timestamp, String activity, String scrolledElement) {
        boolean lastEntryEqual = increasePreviousEntryCountIfEqual(new ScrollDataEntry(null, activity, scrolledElement));
        if (!lastEntryEqual) {
            AppUsageDataEntry entry = new ScrollDataEntry(timestamp, activity, scrolledElement);
            this.dataEntries.add(entry);
            return true;
        }
        else
            return false;
    }

    /**
     * Adds a scroll data entry, using the current time when creating the timestamp
     * @param activity         Activity detected
     * @param scrolledElement  Element scrolled
     * @return True if there was a previous scroll data entry, false otherwise
     */
    public boolean addScrollDataEntry(String activity, String scrolledElement) {
        Date timestamp = new Date();
        return addScrollDataEntry(timestamp, activity, scrolledElement);
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
     * Returns an array with each data entry converted to a String
     * @return An array of all data entries converted to Strings
     */
    public String[] toStrings() {
        String[] result = new String[this.dataEntries.size()];
        int i = 0;
        for (AppUsageDataEntry entry : this.dataEntries) {
            result[i++] = entry.toString();
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
     * Returns the data entries, each containing detailed information
     */
    public LinkedList<AppUsageDataEntry> getDataEntries() {
        return dataEntries;
    }

    /**
     * If the previous layout entry of the is equal to the given one, that entry's
     * count is increased. If the given entry and the last processed entry are both
     * click entries, and they're equal, the last processed entry's count is increased.
     * @param other    Entry to compare the previous same-type entry with
     * @return True if the entries are equal, false otherwise
     */
    private boolean increasePreviousEntryCountIfEqual(AppUsageDataEntry other) {
        AppUsageDataEntry previousEntry = null;
        if (other instanceof ScrollDataEntry) {
            AppUsageDataEntry last = this.dataEntries.peekLast();
            if (last != null && last instanceof ScrollDataEntry)
                previousEntry = last;
        }
        else if (other instanceof ClickDataEntry) {
            AppUsageDataEntry last = this.dataEntries.peekLast();
            if (last != null && last instanceof ClickDataEntry)
                previousEntry = last;
        }
        else if (other instanceof LayoutDataEntry) {
            previousEntry = findLastEntryOfType(LayoutDataEntry.class);
        }
        else if (other instanceof ActivityDataEntry) {
            previousEntry = findLastEntryOfType(ActivityDataEntry.class);
        }

        if (previousEntry != null && previousEntry.equals(other)) {
            previousEntry.increaseCount();
            return true;
        }
        else
            return false;
    }

    /**
     * Returns the last layout entry found, or null if none is found
     */
    private AppUsageDataEntry findLastEntryOfType(Class<?> classType) {
        Iterator<AppUsageDataEntry> it = this.dataEntries.descendingIterator();
        while (it.hasNext()) {
            AppUsageDataEntry entry = it.next();
            if (entry.getClass().equals(classType)) {
                return entry;
            }
        }
        return null;
    }
}
