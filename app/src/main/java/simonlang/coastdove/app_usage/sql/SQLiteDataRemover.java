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

package simonlang.coastdove.app_usage.sql;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.LinkedList;
import java.util.List;

/**
 * Removes an AppUsageData object from the SQLite database
 */
public class SQLiteDataRemover implements Runnable {
    /** App context */
    private Context context;
    /** Primary keys of the app usage data objects to delete */
    private List<Integer> appIDs;

    /**
     * Creates an SQLiteDataRemover for deletion of one AppUsageData
     * @param context    App context
     * @param appID      Primary key of the AppUsageData to delete
     */
    public SQLiteDataRemover(Context context, int appID) {
        this.context = context;
        this.appIDs = new LinkedList<>();
        this.appIDs.add(appID);
    }

    /**
     * Creates an SQLiteDataRemover for deletion of several AppUsageData
     * @param context    App context
     * @param appIDs     Primary key of the AppUsageData objects to delete
     */
    public SQLiteDataRemover(Context context, List<Integer> appIDs) {
        this.context = context;
        this.appIDs = new LinkedList<>(appIDs);
    }

    @Override
    public void run() {
        AppUsageDbHelper dbHelper = new AppUsageDbHelper(this.context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            removeAppUsageData(db);
            List<Integer> activityIDs = removeActivityData(db, this.appIDs);
            List<Integer> dataEntryIDs = removeDataEntries(db, activityIDs);
            removeDetails(db, dataEntryIDs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Removes all entries in AppTable with any matching appID
     */
    private void removeAppUsageData(SQLiteDatabase db) {
        String selection = AppUsageContract.AppTable._ID + "=?";
        for (int appID : appIDs) {
//            Log.d("SQLiteDataRemover", "Removing App ID " + appID);
            String[] selectionArgs = { ""+appID };
            db.delete(AppUsageContract.AppTable.TABLE_NAME, selection, selectionArgs);
        }
    }

    /**
     * Removes all entries in ActivityTable with any matching appID
     * @return Primary keys of all activities removed
     */
    private List<Integer> removeActivityData(SQLiteDatabase db, List<Integer> appIDs) {
        List<Integer> activityIDs = new LinkedList<>();

        String[] projection = { AppUsageContract.ActivityTable._ID };
        String selection = AppUsageContract.ActivityTable.COLUMN_NAME_APP_ID + "=?";
        for (int appID : appIDs) {
            String[] selectionArgs = { ""+appID };
            Cursor c = db.query(AppUsageContract.ActivityTable.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                activityIDs.add(c.getInt(0));
                c.moveToNext();
            }
//            Log.d("SQLiteDataRemover", "Removing ActivityID " + c.getInt(0));
            c.close();
            db.delete(AppUsageContract.ActivityTable.TABLE_NAME, selection, selectionArgs);
        }

//        Log.d("SQLiteDataRemover", "Activity IDs: " + activityIDs.toString());
        return activityIDs;
    }

    /**
     * Removes all entries in DataEntryTable with any matching activityID
     * @param activityIDs    Activity primary keys
     * @return Primary keys of all data entries removed
     */
    private List<Integer> removeDataEntries(SQLiteDatabase db, List<Integer> activityIDs) {
        List<Integer> dataEntryIDs = new LinkedList<>();
//        Log.d("SQLiteDataRemover", "Activity IDs: " + activityIDs.toString());

        String[] projection = { AppUsageContract.DataEntryTable._ID };
        String selection = AppUsageContract.DataEntryTable.COLUMN_NAME_ACTIVITY_ID + "=?";
        for (int activityID : activityIDs) {
            String[] selectionArgs = { ""+activityID };
            Cursor c = db.query(AppUsageContract.DataEntryTable.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                dataEntryIDs.add(c.getInt(0));
                c.moveToNext();
            }
            c.close();
            db.delete(AppUsageContract.DataEntryTable.TABLE_NAME, selection, selectionArgs);
        }

        return dataEntryIDs;
    }

    /**
     * Removes all entries in InteractionDetailsTable and LayoutDetailsTable with any matching dataEntryID
     * @param dataEntryIDs    DataEntry primary keys
     */
    private void removeDetails(SQLiteDatabase db, List<Integer> dataEntryIDs) {
        String selection = AppUsageContract.InteractionDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + "=?";
        String selection2 = AppUsageContract.LayoutDetailsTable.COLUMN_NAME_DATA_ENTRY_ID + "=?";
        for (int dataEntryID : dataEntryIDs) {
            String[] selectionArgs = { ""+dataEntryID };
            db.delete(AppUsageContract.InteractionDetailsTable.TABLE_NAME, selection, selectionArgs);
            db.delete(AppUsageContract.LayoutDetailsTable.TABLE_NAME, selection2, selectionArgs);
        }
    }
}
