/*  Coast Dove
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

package simonlang.coastdove.app_usage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.Date;

import simonlang.coastdove.app_usage.sql.AppUsageContract;
import simonlang.coastdove.utility.Misc;

/**
 * Entry for when the screen turns off
 */
public class ScreenOffEntry extends ActivityDataEntry {

    public static ActivityDataEntry fromSQLiteDB(SQLiteDatabase db, Date timestamp, String activity,
                                                 int count, int dataEntryID) {
        String[] projection = {
                AppUsageContract.ScreenOffDetailsTable.COLUMN_NAME_DATA_ENTRY_ID,
                AppUsageContract.ScreenOffDetailsTable.COLUMN_NAME_DURATION
        };
        String selection = AppUsageContract.ScreenOffDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + "=?";
        String[] selectionArgs = { ""+dataEntryID };

        Cursor c = db.query(AppUsageContract.ScreenOffDetailsTable.TABLE_NAME, projection,
                selection, selectionArgs, null, null, null);
        long duration = 0;
        if (c.moveToFirst())
            duration = c.getLong(1);

        c.close();

        ScreenOffEntry dataEntry = new ScreenOffEntry(timestamp, activity);
        dataEntry.count = count;
        dataEntry.duration = duration;
        return dataEntry;
    }

    /** How long the screen was turned off, in milliseconds */
    private long duration;

    /**
     * Creates a new screen off entry
     *
     * @param timestamp Time at which the screen was turned off
     * @param activity  Activity detected
     */
    public ScreenOffEntry(Date timestamp, String activity) {
        super(timestamp, activity);
        this.duration = System.currentTimeMillis();
    }

    /**
     * Stopwatch-like function to stop measuring how long the screen was turned off
     */
    public void finish() {
        this.duration = System.currentTimeMillis() - this.duration;
    }

    @Override
    public long writeToSQLiteDB(SQLiteDatabase db, long activityID) {
        long dataEntryID = super.writeToSQLiteDB(db, activityID);
        ContentValues values = new ContentValues();
        values.put(AppUsageContract.ScreenOffDetailsTable.COLUMN_NAME_DATA_ENTRY_ID, dataEntryID);
        values.put(AppUsageContract.ScreenOffDetailsTable.COLUMN_NAME_DURATION, duration);

        long rowId = db.insert(AppUsageContract.ScreenOffDetailsTable.TABLE_NAME, null, values);
        if (rowId == -1)
            throw new SQLiteException("Unable to add row to " + AppUsageContract.ScreenOffDetailsTable.TABLE_NAME + ": "
                    + values.toString());
        return dataEntryID;
    }

    @Override
    public String getType() {
        return EntryType.SCREEN_OFF.name();
    }

    @Override
    public String getTypePretty() {
        return EntryType.SCREEN_OFF.toString();
    }

    @Override
    public String getContent() {
        return Misc.msToDurationString(duration);
    }
}
