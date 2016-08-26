package de.uni_bonn.detectappscreen.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.Loader;

import java.util.ArrayList;
import java.util.Collections;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;
import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageDbHelper;

/**
 * Loads a list of entries from an SQLite table
 */
public class SQLiteTableLoader extends Loader<ArrayList<String>> {
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
     * Loads the list of files in the external storage public directory of the given app name, usually
     * /sdcard/{appPackageName}/{subDirectory}/, and delivers the result
     */
    @Override
    public void onStartLoading() {
        AppUsageDbHelper dbHelper = new AppUsageDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
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

        ArrayList<String> data = new ArrayList<>(c.getCount());
        c.moveToFirst();
        while (!c.isAfterLast()) {
            int index = c.getColumnIndex(AppUsageContract.AppTable.COLUMN_NAME_TIMESTAMP);
            data.add(c.getString(index));

            c.moveToNext();
        }

        //Collections.sort(data, new CollatorWrapper());
        deliverResult(data);
        dbHelper.close();
    }
}
