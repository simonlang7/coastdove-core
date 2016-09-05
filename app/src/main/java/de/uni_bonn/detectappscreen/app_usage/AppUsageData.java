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
import android.content.Context;
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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;
import de.uni_bonn.detectappscreen.detection.AppDetectionData;
import de.uni_bonn.detectappscreen.detection.AppMetaInformation;
import de.uni_bonn.detectappscreen.utility.CollatorWrapper;
import de.uni_bonn.detectappscreen.utility.FileHelper;
import de.uni_bonn.detectappscreen.utility.Misc;

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
                AppUsageContract.ActivityTable.COLUMN_NAME_LEVEL,
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
                timestamp = new SimpleDateFormat(Misc.DATE_TIME_FORMAT).parse(c.getString(1));
            } catch (ParseException e) {
                throw new RuntimeException("Cannot parse date: " + c.getString(1));
            }
            String activity = c.getString(3);
            int level = c.getInt(4);
            long duration = c.getLong(5);

            // add activity data
            result.activityDataList.add(ActivityData.fromSQLiteDB(db, appPackageName, timestamp,
                    activity, activityID, level, duration));

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
        this.duration = new Date().getTime();
    }

    /**
     * Stopwatch-like function to stop measuring duration for this session
     */
    public void finish() {
        this.duration = new Date().getTime() - this.duration;
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
     * Converts all data contained to strings with padding appropriate to the activities' levels
     * @return All ActivityData as strings
     */
    public String[] toStrings() {
        List<String> strings = new LinkedList<>();
        for (ActivityData activityData : activityDataList) {
            int level = activityData.getLevel();
            String[] activityDataStrings = activityData.toStrings(2*level);
            for (String string : activityDataStrings) {
                strings.add(string);
            }
        }
        return strings.toArray(new String[strings.size()]);
    }

    /**
     * Returns the filename for saving this object
     */
    public String getTextFilename() {
        if (this.activityDataList.size() == 0)
            return null;
        Date first = this.activityDataList.get(0).getTimestamp();
        String format = new SimpleDateFormat(Misc.DATE_TIME_FILENAME).format(first);
        return format + ".txt";
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
