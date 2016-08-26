package de.uni_bonn.detectappscreen.app_usage.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import de.uni_bonn.detectappscreen.app_usage.AppUsageData;

/**
 * Es ist zu heiss fuer Doku todo: zu heiss fuer todos
 */
public class SQLiteWriter implements Runnable {
    private Context context;
    private AppUsageData appUsageData;

    public SQLiteWriter(Context context, AppUsageData appUsageData) {
        this.context = context;
        this.appUsageData = appUsageData;
    }

    @Override
    public void run() {
        AppUsageDbHelper helper = new AppUsageDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {
            appUsageData.writeToSQLiteDB(db);
            db.setTransactionSuccessful();
        } catch (RuntimeException e) {
            Log.e("SQLiteWriter", e.getMessage());
        } finally {
            db.endTransaction();
        }
    }
}
