package de.uni_bonn.detectappscreen.app_usage;

import android.content.ContentValues;
import android.database.Cursor;
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
 * of the types LayoutDataEntry and InteractionDataEntry
 */
public class ActivityData {
    public static final String DATE_DETAILED = "yyyy-MM-dd HH:mm:ss:SSS";

    public static ActivityData fromSQLiteDB(SQLiteDatabase db, String appPackageName,
                                            Date timestamp, String activity, int activityID,
                                            long duration) {
        ActivityData result = new ActivityData(appPackageName, timestamp, activity);
        result.duration = duration;

        String[] projection = {
                AppUsageContract.DataEntryTable._ID,
                AppUsageContract.DataEntryTable.COLUMN_NAME_TIMESTAMP,
                AppUsageContract.DataEntryTable.COLUMN_NAME_ACTIVITY_ID,
                AppUsageContract.DataEntryTable.COLUMN_NAME_COUNT,
                AppUsageContract.DataEntryTable.COLUMN_NAME_TYPE
        };
        String selection = AppUsageContract.DataEntryTable.COLUMN_NAME_ACTIVITY_ID + "=?";
        String[] selectionArgs = { ""+activityID };
        String sortOrder = AppUsageContract.DataEntryTable.COLUMN_NAME_TIMESTAMP + " ASC";

        Cursor c = db.query(AppUsageContract.DataEntryTable.TABLE_NAME, projection,
                selection, selectionArgs, null, null, sortOrder);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            int dataEntryID = c.getInt(0);
            Date entryTimestamp;
            try {
                entryTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").parse(c.getString(1));
            } catch (ParseException e) {
                throw new RuntimeException("Cannot parse date: " + c.getString(1));
            }
            int count = c.getInt(3);
            String type = c.getString(4);

            ActivityDataEntry.EntryType entryType = ActivityDataEntry.entryTypeFromString(type);
            switch (entryType) {
                case CLICK:
                case LONG_CLICK:
                case SCROLLING:
                    result.dataEntries.add(InteractionDataEntry.fromSQLiteDB(db, entryTimestamp, activity,
                            entryType, count, dataEntryID));
                    break;
                case LAYOUTS:
                    result.dataEntries.add(LayoutDataEntry.fromSQLiteDB(db, entryTimestamp, activity,
                            count, dataEntryID));
                    break;
            }

            c.moveToNext();
        }
        c.close();

        return result;
    }

    /** Package name of the app */
    private String appPackageName;
    /** Time at which this activity was activated */
    private Date timestamp;
    /** Activity detected */
    private String activity;
    /** List of data entries that were collected during this activity */
    private LinkedList<ActivityDataEntry> dataEntries;
    /** Duration of this activity, in milliseconds */
    private long duration;

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
        start();
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

                ActivityDataEntry dataEntry = null;
                if (dataEntryJSON.has("detectedLayouts"))
                    dataEntry = new LayoutDataEntry(dataEntryJSON);
                else if (dataEntryJSON.has("detectedClick"))
                    dataEntry = new InteractionDataEntry(dataEntryJSON);

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
    public List<ActivityDataEntry> getDataEntries() {
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
            for (ActivityDataEntry entry : dataEntries)
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

        for (ActivityDataEntry entry : dataEntries)
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
            ActivityDataEntry entry = new LayoutDataEntry(timestamp, activity, detectedLayouts);
            this.dataEntries.add(entry);
            return true;
        }
        else
            return false;
    }

    /**
     * Adds an interaction data entry
     * @param timestamp        Time at which the data were collected
     * @param activity         Activity detected
     * @param detectedInteraction    Interaction detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     */
    public boolean addInteractionDataEntry(Date timestamp, String activity, Set<InteractionEventData> detectedInteraction,
                                           ActivityDataEntry.EntryType type) {
        boolean lastEntryEqual = increasePreviousEntryCountIfEqual(new InteractionDataEntry(null, activity, detectedInteraction,
                type));
        if (!lastEntryEqual) {
            ActivityDataEntry entry = new InteractionDataEntry(timestamp, activity, detectedInteraction, type);
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
        for (ActivityDataEntry entry : this.dataEntries) {
            result[i++] = entry.toString(padding);
        }
        return result;
    }

    /**
     * Stopwatch-like function to start measuring duration for this activity
     */
    public void start() {
        this.duration = System.currentTimeMillis();
    }

    /**
     * Stopwatch-like function to stop measuring duration for this activity
     */
    public void finish() {
        this.duration = System.currentTimeMillis() - this.duration;
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
    private boolean increasePreviousEntryCountIfEqual(ActivityDataEntry other) {
        ActivityDataEntry previousEntry = null;
        if (other instanceof InteractionDataEntry) {
            ActivityDataEntry last = this.dataEntries.peekLast();
            if (last != null && last instanceof InteractionDataEntry)
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
    private ActivityDataEntry findLastEntryOfType(Class<?> classType) {
        Iterator<ActivityDataEntry> it = this.dataEntries.descendingIterator();
        while (it.hasNext()) {
            ActivityDataEntry entry = it.next();
            if (entry.getClass().equals(classType)) {
                return entry;
            }
        }
        return null;
    }
}
