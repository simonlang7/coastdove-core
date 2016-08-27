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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;
import de.uni_bonn.detectappscreen.utility.CollatorWrapper;

/**
 * Data entry containing a scroll event at a certain point during app usage
 */
public class ScrollDataEntry extends AppUsageDataEntry {

    public static AppUsageDataEntry fromSQLiteDB(SQLiteDatabase db, Date timestamp, String activity,
                                                 int count, int dataEntryID) {
        String[] projection = {
                AppUsageContract.ScrollDetailsTable.COLUMN_NAME_DATA_ENTRY_ID,
                AppUsageContract.ScrollDetailsTable.COLUMN_NAME_ELEMENT
        };
        String selection = AppUsageContract.ScrollDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + "=?";
        String[] selectionArgs = { ""+dataEntryID };

        Cursor c = db.query(AppUsageContract.ScrollDetailsTable.TABLE_NAME, projection,
                selection, selectionArgs, null, null, null);
        c.moveToFirst();
        Set<String> scrolledElements = new TreeSet<>(new CollatorWrapper());
        while (!c.isAfterLast()) {
            String element = c.getString(1);
            scrolledElements.add(element);

            c.moveToNext();
        }

        ScrollDataEntry dataEntry = new ScrollDataEntry(timestamp, activity, scrolledElements.toString());
        dataEntry.count = count;
        return dataEntry;
    }

    /** Element scrolled */
    private String scrolledElement;

    public ScrollDataEntry(Date timestamp, String activity, String scrolledElement) {
        super(timestamp, activity);
        this.scrolledElement = scrolledElement;
    }

    public ScrollDataEntry(JSONObject entryJSON) {
        super(entryJSON);
        try {
            this.scrolledElement = entryJSON.getString("scrolledElement");
        } catch (JSONException e) {
            Log.e("ScrollDataEntry", "Unable to read from JSONObject: " + e.getMessage());
        }
    }


    @Override
    public boolean equals(AppUsageDataEntry other) {
        if (!super.equals(other))
            return false;

        if (!(other instanceof ScrollDataEntry))
            return false;

        ScrollDataEntry otherEntry = (ScrollDataEntry)other;
        if (!this.scrolledElement.equals(otherEntry.scrolledElement))
            return false;

        return true;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject result = super.toJSON();
        try {
            result.put("scrolledElement", this.scrolledElement);
        } catch (JSONException e) {
            Log.e("ScrollDataEntry", "Unable to create JSONObject: " + e.getMessage());
        }
        return result;
    }

    @Override
    public long writeToSQLiteDB(SQLiteDatabase db, long activityID) {
        long dataEntryID = super.writeToSQLiteDB(db, activityID);

        ContentValues values = new ContentValues();
        values.put(AppUsageContract.ScrollDetailsTable.COLUMN_NAME_DATA_ENTRY_ID, dataEntryID);
        values.put(AppUsageContract.ScrollDetailsTable.COLUMN_NAME_ELEMENT, this.scrolledElement);

        long rowId = db.insert(AppUsageContract.ScrollDetailsTable.TABLE_NAME, null, values);
        if (rowId == -1)
            throw new SQLiteException("Unable to add row to " + AppUsageContract.ScrollDetailsTable.TABLE_NAME + ": "
                    + values.toString());

        return dataEntryID;
    }

    @Override
    public String getType() {
        return "Scrolling";
    }

    @Override
    public String getContent() {
        return this.scrolledElement;
    }
}
