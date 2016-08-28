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
import android.database.sqlite.SQLiteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;

/**
 * Data entry containing a detected click at a certain point during app usage
 */
public class ClickDataEntry extends ActivityDataEntry {
    public static ActivityDataEntry fromSQLiteDB(SQLiteDatabase db, Date timestamp, String activity,
                                                 int count, int dataEntryID) {
        String[] projection = {
                AppUsageContract.ClickDetailsTable.COLUMN_NAME_DATA_ENTRY_ID,
                AppUsageContract.ClickDetailsTable.COLUMN_NAME_ANDROID_ID,
                AppUsageContract.ClickDetailsTable.COLUMN_NAME_TEXT,
                AppUsageContract.ClickDetailsTable.COLUMN_NAME_CLASS_NAME
        };
        String selection = AppUsageContract.ClickDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + "=?";
        String[] selectionArgs = { ""+dataEntryID };
        //String sortOrder = AppUsageContract.ClickDetailsTable.COLUMN_NAME_TIMESTAMP + " ASC";

        Cursor c = db.query(AppUsageContract.ClickDetailsTable.TABLE_NAME, projection,
                selection, selectionArgs, null, null, null);
        c.moveToFirst();
        Set<ClickedEventData> click = new CopyOnWriteArraySet<>();
        while (!c.isAfterLast()) {
            String androidID = c.getString(1);
            String text = c.getString(2);
            String className = c.getString(3);
            ClickedEventData eventData = new ClickedEventData(androidID, text, className);
            click.add(eventData);

            c.moveToNext();
        }

        ClickDataEntry dataEntry = new ClickDataEntry(timestamp, activity, click);
        dataEntry.count = count;
        return dataEntry;
    }

    /** Elements clicked */
    private Set<ClickedEventData> detectedClick;

    public ClickDataEntry(Date timestamp, String activity, Set<ClickedEventData> detectedClick) {
        super(timestamp, activity);
        this.detectedClick = detectedClick;
    }

    public ClickDataEntry(JSONObject entryJSON) {
        super(entryJSON);

        this.detectedClick = new CopyOnWriteArraySet<>();
        try {
            JSONArray detectedClickJSON = entryJSON.getJSONArray("detectedClick");
            for (int i = 0; i < detectedClickJSON.length(); ++i) {
                JSONObject clickDataJSON = detectedClickJSON.getJSONObject(i);
                ClickedEventData clickData = new ClickedEventData(clickDataJSON);
                this.detectedClick.add(clickData);
            }
        } catch (JSONException e) {
            Log.e("ClickDataEntry", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    @Override
    public boolean equals(ActivityDataEntry other) {
        if (!super.equals(other))
            return false;

        if (!(other instanceof ClickDataEntry))
            return false;

        ClickDataEntry otherEntry = (ClickDataEntry)other;
        if (this.detectedClick.size() != otherEntry.detectedClick.size())
            return false;

        outer: for (ClickedEventData clickData : this.detectedClick) {
            for (ClickedEventData otherClickData : otherEntry.detectedClick) {
                if (clickData.equals(otherClickData))
                    continue outer;
            }
            return false;
        }

        return true;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject result = super.toJSON();
        try {
            JSONArray detectedClickJSON = new JSONArray();
            for (ClickedEventData clickData : detectedClick)
                detectedClickJSON.put(clickData.toJSON());
            result.put("detectedClick", detectedClickJSON);
        } catch (JSONException e) {
            Log.e("ClickDataEntry", "Unable to create JSONObject: " + e.getMessage());
        }
        return result;
    }

    @Override
    public long writeToSQLiteDB(SQLiteDatabase db, long activityID) {
        long dataEntryID = super.writeToSQLiteDB(db, activityID);
        for (ClickedEventData data : this.detectedClick) {
            ContentValues values = data.toContentValues(dataEntryID);
            Log.d("ClickDataEntry", "Adding values: " + values.toString());
            long rowId = db.insert(AppUsageContract.ClickDetailsTable.TABLE_NAME, null, values);
            if (rowId == -1)
                throw new SQLiteException("Unable to add row to " + AppUsageContract.ClickDetailsTable.TABLE_NAME + ": "
                    + values.toString());
        }
        return dataEntryID;
    }

    @Override
    public String getType() {
        return "Click";
    }

    @Override
    public String getContent() {
        return detectedClick.toString();
    }

    public Set<ClickedEventData> getDetectedClick() {
        return this.detectedClick;
    }
}
