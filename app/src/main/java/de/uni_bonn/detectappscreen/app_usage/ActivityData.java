package de.uni_bonn.detectappscreen.app_usage;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;

/**
 * Data collected in any one activity, usually contains several app usage data entries
 * of the types ScrollDataEntry, LayoutDataEntry, and ClickDataEntry
 */
public class ActivityData {
    public static final String DATE_DETAILED = "yyyy-MM-dd HH:mm:ss:SSS";

    /** Package name of the app */
    private String appPackageName;
    /** Time at which this activity was activated */
    private Date timestamp;
    /** Activity detected */
    private String activity;
    /** List of data entries that were collected during this activity */
    private LinkedList<AppUsageDataEntry> dataEntries;

    /**
     * Creates a new activity data object
     * @param appPackageName     Package name of the app
     * @param timestamp          Time at which the data were collected
     * @param activity           Activity detected
     */
    public ActivityData(String appPackageName, Date timestamp, String activity) {
        this.appPackageName = appPackageName;
        this.timestamp = timestamp;
        this.activity = activity;
        this.dataEntries = new LinkedList<>();
    }

    /**
     * Creates a new activity data object
     * @param dataJSON Activity data in JSON format
     */
    public ActivityData(JSONObject dataJSON) {
        this.appPackageName = "";
        this.activity = "";
        this.dataEntries = new LinkedList<>();
        try {
            this.appPackageName = dataJSON.getString("package");
            this.activity = dataJSON.getString("activity");
            String timestamp = dataJSON.getString("timestamp");
            this.timestamp = new SimpleDateFormat(DATE_DETAILED).parse(timestamp);
            JSONArray dataEntriesJSON = dataJSON.getJSONArray("dataEntries");
            for (int i = 0; i < dataEntriesJSON.length(); ++i) {
                JSONObject dataEntryJSON = dataEntriesJSON.getJSONObject(i);

                AppUsageDataEntry dataEntry = null;
                if (dataEntryJSON.has("detectedLayouts"))
                    dataEntry = new LayoutDataEntry(dataEntryJSON);
                else if (dataEntryJSON.has("detectedClick"))
                    dataEntry = new ClickDataEntry(dataEntryJSON);
                else if (dataEntryJSON.has("scrolledElement"))
                    dataEntry = new ScrollDataEntry(dataEntryJSON);

                if (dataEntry != null)
                    this.dataEntries.add(dataEntry);
            }
        } catch (JSONException e) {
            Log.e("ActivityData", "Unable to read from JSONObject: " + e.getMessage());
        } catch (ParseException e) {
            Log.e("ActivityData", "Unable to parse from SimpleDateFormat: " + e.getMessage());
        }
    }

    /** Time at which this activity was activated */
    public Date getTimestamp() {
        return timestamp;
    }

    /** Returns the timestamp as a formatted string */
    public String getTimestampString() {
        return new SimpleDateFormat(DATE_DETAILED).format(this.timestamp);
    }

    /** Activity detected */
    public String getActivity() {
        return activity;
    }

    /** Activity detected, shortened String (omits everything up to and including the first '/') */
    public String getShortenedActivity() {
        return activity.replaceAll(".*/", "");
    }

    /** List of data entries that were collected during this activity */
    public List<AppUsageDataEntry> getDataEntries() {
        return dataEntries;
    }


    /**
     * Converts the ActivityData object to JSON and returns the according JSONObject
     */
    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        try {
            result.put("_type", "ActivityData");
            result.put("package", this.appPackageName);
            result.put("activity", this.activity);
            result.put("timestamp", getTimestampString());

            JSONArray dataEntriesJSON = new JSONArray();
            for (AppUsageDataEntry entry : dataEntries)
                dataEntriesJSON.put(entry.toJSON());

            result.put("dataEntries", dataEntriesJSON);
        } catch (JSONException e) {
            Log.e("ActivityData", "Unable to create JSONObject for " + this.appPackageName + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Writes the contents of this object to an SQLite database
     * @param db       Database to write to
     * @param appID    Primary key of the associated app (AppUsageData)
     */
    public void writeToSQLiteDB(SQLiteDatabase db, long appID) {
        ContentValues values = new ContentValues();
        values.put(AppUsageContract.ActivityTable.COLUMN_NAME_TIMESTAMP, getTimestampString());
        values.put(AppUsageContract.ActivityTable.COLUMN_NAME_APP_ID, appID);
        values.put(AppUsageContract.ActivityTable.COLUMN_NAME_ACTIVITY, this.activity);

        long rowId = db.insert(AppUsageContract.ActivityTable.TABLE_NAME, null, values);
        if (rowId == -1)
            throw new SQLiteException("Unable to add row to " + AppUsageContract.ActivityTable.TABLE_NAME + ": "
                    + values.toString());

        for (AppUsageDataEntry entry : dataEntries)
            entry.writeToSQLiteDB(db, rowId);
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
     * Returns an array with each data entry data converted to a String
     * @return An array of all data entries converted to Strings
     */
    public String[] toStrings(int padding) {
        String[] result = new String[this.dataEntries.size() + 1];
        result[0] = toString(padding);
        int i = 1;
        for (AppUsageDataEntry entry : this.dataEntries) {
            result[i++] = entry.toString(padding);
        }
        return result;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int padding) {
        String timestamp = getTimestampString();
        String paddingString = " ";
        for (int i = 0; i < padding; ++i)
            paddingString += " ";
        return timestamp + paddingString + "Activity: " + getShortenedActivity();
    }

    /** Returns a unique ID for this data entry */
    public long id() {
        return timestamp.getTime();
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
