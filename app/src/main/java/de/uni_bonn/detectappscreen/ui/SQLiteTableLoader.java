package de.uni_bonn.detectappscreen.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayList;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;
import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageDbHelper;

/**
 * Loads a list of entries from an SQLite table
 */
public class SQLiteTableLoader extends Loader<ArrayList<Pair<Integer, String>>> {
    /** Name of the app, needed for the external storage public directory */
    private String appPackageName;

    /**
     * Creates an SQLiteTableLoader with the given context and app package name
     *
     * @param context Used to retrieve the application context.
     */
    public SQLiteTableLoader(Context context, String appPackageName) {
        super(context);
        this.appPackageName = appPackageName;
    }

    /**
     * Loads the list of app usage data collections for this object's appPackageName
     */
    @Override
    public void onStartLoading() {
        // Open database
        AppUsageDbHelper dbHelper = new AppUsageDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                AppUsageContract.AppTable._ID,
                AppUsageContract.AppTable.COLUMN_NAME_TIMESTAMP,
                AppUsageContract.AppTable.COLUMN_NAME_PACKAGE
        };
        String selection = AppUsageContract.AppTable.COLUMN_NAME_PACKAGE + "=?";
        String[] selectionArgs = {
                this.appPackageName
        };
        String sortOrder = AppUsageContract.AppTable.COLUMN_NAME_TIMESTAMP + " ASC";

        Cursor c = db.query(AppUsageContract.AppTable.TABLE_NAME,
                projection, selection, selectionArgs, null, null, sortOrder);

        // Extract all data from the cursor, so we can close the database
        ArrayList<Pair<Integer, String>> data = new ArrayList<>(c.getCount());
        c.moveToFirst();
        while (!c.isAfterLast()) {
            int timestampIndex = c.getColumnIndex(AppUsageContract.AppTable.COLUMN_NAME_TIMESTAMP);
            int idIndex = c.getColumnIndex(AppUsageContract.AppTable._ID);
            Log.d("SQLiteTableLoader", "(timestamp, id) = (" + timestampIndex + ", " + idIndex + ")");
            data.add(new Pair<Integer, String>(c.getInt(idIndex), c.getString(timestampIndex)));

            c.moveToNext();
        }

        //Collections.sort(data, new CollatorWrapper());
        deliverResult(data);
        dbHelper.close();
    }
}
