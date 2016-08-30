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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Set;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;

/**
 * Data collected from app usage, typically contains a list of timestamps associated
 * with an activity and a list of layouts detected at that time
 */
public class AppUsageData {
    public static AppUsageData fromSQLiteDB(SQLiteDatabase db, String appPackageName, int appID) {
        AppUsageData result = new AppUsageData(appPackageName);

        String[] projection = {
                AppUsageContract.ActivityTable._ID,
                AppUsageContract.ActivityTable.COLUMN_NAME_TIMESTAMP,
                AppUsageContract.ActivityTable.COLUMN_NAME_APP_ID,
                AppUsageContract.ActivityTable.COLUMN_NAME_ACTIVITY,
                AppUsageContract.ActivityTable.COLUMN_NAME_DURATION
        };
        String selection = AppUsageContract.ActivityTable.COLUMN_NAME_APP_ID + "=?";
        String[] selectionArgs = { ""+appID };
        String sortOrder = AppUsageContract.ActivityTable.COLUMN_NAME_TIMESTAMP + " ASC";

        Cursor c = db.query(AppUsageContract.ActivityTable.TABLE_NAME,
                projection, selection, selectionArgs, null, null, sortOrder);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            int activityID = c.getInt(0);
            Date timestamp;
            try {
                timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").parse(c.getString(1));
            } catch (ParseException e) {
                throw new RuntimeException("Cannot parse date: " + c.getString(1));
            }
            String activity = c.getString(3);
            long duration = c.getLong(4);

            // add activity data
            result.activityDataList.add(ActivityData.fromSQLiteDB(db, appPackageName, timestamp,
                    activity, activityID, duration));

            c.moveToNext();
        }
        c.close();

        return result;
    }


    /** Name of the package associated with these app usage data */
    private String appPackageName;
    /** Activity data, each object containing data entries with detailed information */
    private LinkedList<ActivityData> activityDataList;
    /** Duration of this session, in milliseconds */
    private long duration;

    /**
     * Constructs an AppUsageData object with the given package name and an empty list
     * of data entries
     * @param appPackageName    Name of the app / package associated
     */
    public AppUsageData(String appPackageName) {
        this.appPackageName = appPackageName;
        this.activityDataList = new LinkedList<>();
        start();
    }

    /**
     * Constructs an AppUsageData object from a JSONObject and fills it with the included
     * data
     * @param dataJSON    JSONObject containing the data
     */
    public AppUsageData(JSONObject dataJSON) {
        this.appPackageName = "";
        this.activityDataList = new LinkedList<>();
        try {
            this.appPackageName = dataJSON.getString("package");
            JSONArray activityDataListJSON = dataJSON.getJSONArray("activityDataList");
            for (int i = 0; i < activityDataListJSON.length(); ++i) {
                JSONObject activityDataJSON = activityDataListJSON.getJSONObject(i);
                ActivityData activityData = new ActivityData(activityDataJSON);
                this.activityDataList.add(activityData);
            }
        } catch (JSONException e) {
            Log.e("AppUsageData", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    /**
     * Adds an activity data object
     * @param timestamp    Time at which the data were collected
     * @param activity     Activity detected
     * @return True if a new activity data object was added, false otherwise
     */
    public boolean addActivityData(Date timestamp, String activity) {
        if (!activityDataList.isEmpty()) {
            ActivityData previousData = activityDataList.peekLast();
            if (previousData.getActivity().contains(activity))
                return false;
            else
                previousData.finish();
        }
        ActivityData data = new ActivityData(this.appPackageName, timestamp, activity);
        this.activityDataList.add(data);
        return true;
    }

    /**
     * Adds an activity data object, using the current time when creating the timestamp
     * @param activity           Activity detected
     * @return True if a new activity data object was added, false otherwise
     */
    public boolean addActivityData(String activity) {
        Date timestamp = new Date();
        return addActivityData(timestamp, activity);
    }

    /**
     * Adds a layout data entry
     * @param timestamp          Time at which the data were collected
     * @param activity           Activity detected
     * @param detectedLayouts    Layouts detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     *         or there was no activity data object to add the data to
     */
    public boolean addLayoutDataEntry(Date timestamp, String activity, Set<String> detectedLayouts) {
        if (activityDataList.isEmpty())
            return false;
        return activityDataList.peekLast().addLayoutDataEntry(timestamp, activity, detectedLayouts);
    }

    /**
     * Adds a layout data entry, using the current time when creating the timestamp
     * @param activity           Activity detected
     * @param detectedLayouts    Layouts detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     *         or there was no activity data object to add the data to
     */
    public boolean addLayoutDataEntry(String activity, Set<String> detectedLayouts) {
        Date timestamp = new Date();
        return addLayoutDataEntry(timestamp, activity, detectedLayouts);
    }

    /**
     * Adds an interaction data entry
     * @param timestamp        Time at which the data were collected
     * @param activity         Activity detected
     * @param detectedInteraction    Interaction detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     *         or there was no activity data object to add the data to
     */
    public boolean addInteractionDataEntry(Date timestamp, String activity, Set<InteractionEventData> detectedInteraction,
                                           ActivityDataEntry.EntryType type) {
        if (activityDataList.isEmpty())
            return false;
        return activityDataList.peekLast().addInteractionDataEntry(timestamp, activity, detectedInteraction, type);
    }

    /**
     * Adds an interaction data entry, using the current time when creating the timestamp
     * @param activity         Activity detected
     * @param detectedInteraction    Interaction detected
     * @return True if a new data entry was added, false if the previous data entry equals these data
     *         or there was no activity data object to add the data to
     */
    public boolean addInteractionDataEntry(String activity, Set<InteractionEventData> detectedInteraction, ActivityDataEntry.EntryType type) {
        Date timestamp = new Date();
        return addInteractionDataEntry(timestamp, activity, detectedInteraction, type);
    }

    /**
     * Stopwatch-like function to start measuring duration for this session
     */
    public void start() {
        this.duration = System.currentTimeMillis();
    }

    /**
     * Stopwatch-like function to stop measuring duration for this session
     */
    public void finish() {
        this.duration = System.currentTimeMillis() - this.duration;
    }

    /**
     * Converts the AppUsageData object to JSON and returns the according JSONObject
     */
    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        try {
            result.put("_type", "AppUsageData");
            result.put("package", this.appPackageName);

            JSONArray activityDataJSON = new JSONArray();
            for (ActivityData data : activityDataList)
                activityDataJSON.put(data.toJSON());

            result.put("activityDataList", activityDataJSON);
        } catch (JSONException e) {
            Log.e("AppUsageData", "Unable to create JSONObject for " + this.appPackageName + ": " + e.getMessage());
        }

        return result;
    }

    /**
     * Writes the contained data into an SQLite database
     * @param db    Database to write to
     */
    public void writeToSQLiteDB(SQLiteDatabase db) {
        if (this.activityDataList.isEmpty())
            throw new RuntimeException("No activity data");

        String timestamp = this.activityDataList.getFirst().getTimestampString();
        ContentValues values = new ContentValues();
        values.put(AppUsageContract.AppTable.COLUMN_NAME_PACKAGE, this.appPackageName);
        values.put(AppUsageContract.AppTable.COLUMN_NAME_TIMESTAMP, timestamp);
        values.put(AppUsageContract.AppTable.COLUMN_NAME_DURATION, this.duration);

        long rowId = db.insert(AppUsageContract.AppTable.TABLE_NAME, null, values);
        for (ActivityData data : this.activityDataList)
            data.writeToSQLiteDB(db, rowId);
    }

    /**
     * Returns the filename for saving this object
     */
    public String getFilename() {
        if (this.activityDataList.size() == 0)
            return "empty.json";
        Date first = this.activityDataList.get(0).getTimestamp();
        String format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(first);
        return format + ".json";
    }

    /**
     * Returns the name of the package associated with these data
     */
    public String getAppPackageName() {
        return appPackageName;
    }

    /**
     * Returns the data entries, each containing detailed information
     */
    public LinkedList<ActivityData> getActivityDataList() {
        return activityDataList;
    }
}
