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

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Data entry containing information about one special event during app usage
 */
public abstract class AppUsageDataEntry {
    /** Time at which these data were collected */
    private Date timestamp;
    /** Activity detected */
    private String activity;
    /** Number of consecutive occurrences of this data entry, disregarding the timestamp */
    private int count;

    /**
     * Creates a new app usage data entry
     * @param timestamp          Time at which the data were collected
     * @param activity           Activity detected
     */
    public AppUsageDataEntry(Date timestamp, String activity) {
        this.timestamp = timestamp;
        this.activity = activity;
        this.count = 1;
    }

    /**
     * Creates a new app usage data entry from a JSONObject
     * @param entryJSON An entry in JSON format
     */
    public AppUsageDataEntry(JSONObject entryJSON) {
        this.timestamp = new Date(new GregorianCalendar(0, 1, 1).getTimeInMillis());
        this.activity = "";
        try {
            this.activity = entryJSON.getString("activity");
            String timestamp = entryJSON.getString("timestamp");
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").parse(timestamp);
            this.count = entryJSON.getInt("count");
        } catch (JSONException e) {
            Log.e("AppUsageDataEntry", "Unable to read from JSONObject: " + e.getMessage());
        } catch (ParseException e) {
            Log.e("AppUsageDataEntry", "Unable to parse timestamp: " + e.getMessage());
        }
    }

    /**
     * Compares this entry with another entry, disregarding timestamp and count
     * @param other    Entry to compare with
     * @return True if the entries are equal, false otherwise
     */
    public boolean equals(AppUsageDataEntry other) {
        if (!this.activity.equals(other.activity))
            return false;

        return true;
    }

    /**
     * Increases the number of consecutive occurrences of this data entry (disregarding the timestamp)
     */
    public void increaseCount() {
        ++this.count;
    }

    /**
     * Converts the entry to JSON and returns the according JSONObject
     */
    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        try {
            result.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(this.timestamp));
            result.put("activity", this.activity);
            result.put("count", this.count);
        } catch (JSONException e) {
            Log.e("AppUsageDataEntry", "Unable to create JSONObject: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String toString() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(getTimestamp());
        return timestamp + " " + getShortenedActivity() + ": " + getType() + " (" + getCount() + "): " + getContent();
    }

    /** Type of data entry */
    public abstract String getType();

    /** Content of this data entry as a string */
    public abstract String getContent();

    /** Time at which these data were collected */
    public Date getTimestamp() {
        return timestamp;
    }

    /** Activity detected */
    public String getActivity() {
        return activity;
    }

    /** Activity detected, shortened String (omits everything up to and including the first '/') */
    public String getShortenedActivity() {
        return activity.replaceAll(".*/", "");
    }

    /** Number of consecutive occurrences of this data entry, disregarding the timestamp */
    public int getCount() {
        return count;
    }
}