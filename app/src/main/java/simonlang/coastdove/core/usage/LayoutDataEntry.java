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

package simonlang.coastdove.core.usage;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import simonlang.coastdove.core.usage.sql.AppUsageContract;
import simonlang.coastdove.core.utility.CollatorWrapper;

/**
 * Data entry containing detected layouts at a certain point during app usage
 */
public class LayoutDataEntry extends ActivityDataEntry {

    public static ActivityDataEntry fromSQLiteDB(SQLiteDatabase db, Date timestamp, String activity,
                                                 int count, int dataEntryID) {
        String[] projection = {
                AppUsageContract.LayoutDetailsTable.COLUMN_NAME_DATA_ENTRY_ID,
                AppUsageContract.LayoutDetailsTable.COLUMN_NAME_LAYOUT
        };
        String selection = AppUsageContract.LayoutDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + "=?";
        String[] selectionArgs = { ""+dataEntryID };

        Cursor c = db.query(AppUsageContract.LayoutDetailsTable.TABLE_NAME, projection,
                selection, selectionArgs, null, null, null);
        c.moveToFirst();
        Set<String> layouts = new TreeSet<>(new CollatorWrapper());
        while (!c.isAfterLast()) {
            String layout = c.getString(1);
            layouts.add(layout);

            c.moveToNext();
        }
        c.close();

        LayoutDataEntry dataEntry = new LayoutDataEntry(timestamp, activity, layouts);
        dataEntry.count = count;
        return dataEntry;
    }

    /** Layouts detected */
    private Set<String> detectedLayouts;

    public LayoutDataEntry(Date timestamp, String activity, Set<String> detectedLayouts) {
        super(timestamp, activity);
        this.detectedLayouts = detectedLayouts;
    }

    @Override
    public boolean equals(ActivityDataEntry other) {
        if (!super.equals(other))
            return false;

        if (!(other instanceof LayoutDataEntry))
            return false;

        LayoutDataEntry otherEntry = (LayoutDataEntry)other;
        if (this.detectedLayouts.size() != otherEntry.detectedLayouts.size())
            return false;

        outer: for (String layout : this.detectedLayouts) {
            for (String otherLayout : otherEntry.detectedLayouts) {
                if (layout.equals(otherLayout))
                    continue outer;
            }
            return false;
        }

        return true;
    }

    @Override
    public long writeToSQLiteDB(SQLiteDatabase db, long activityID) {
        long dataEntryID = super.writeToSQLiteDB(db, activityID);
        for (String layout : this.detectedLayouts) {
            ContentValues values = new ContentValues();
            values.put(AppUsageContract.LayoutDetailsTable.COLUMN_NAME_DATA_ENTRY_ID, dataEntryID);
            values.put(AppUsageContract.LayoutDetailsTable.COLUMN_NAME_LAYOUT, layout);

            long rowId = db.insert(AppUsageContract.LayoutDetailsTable.TABLE_NAME, null, values);
            if (rowId == -1)
                throw new SQLiteException("Unable to add row to " + AppUsageContract.LayoutDetailsTable.TABLE_NAME + ": "
                        + values.toString());
        }
        return dataEntryID;
    }

    @Override
    public String getType() {
        return EntryType.LAYOUTS.name();
    }

    @Override
    public String getTypePretty() {
        return EntryType.LAYOUTS.toString();
    }

    @Override
    public String getContent() {
        return detectedLayouts.toString();
    }

    /** Layouts detected */
    public Set<String> getDetectedLayouts() {
        return detectedLayouts;
    }
}
