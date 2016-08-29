package de.uni_bonn.detectappscreen.app_usage.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

/**
 * Removes an AppUsageData object from the SQLite database
 */
public class SQLiteDataRemover implements Runnable {
    private Context context;
    private List<Integer> appIDs;

    public SQLiteDataRemover(Context context, int appID) {
        this.context = context;
        this.appIDs.add(appID);
    }

    public SQLiteDataRemover(Context context, List<Integer> appIDs) {
        this.context = context;
        this.appIDs.addAll(appIDs);
    }

    @Override
    public void run() {
        AppUsageDbHelper dbHelper = new AppUsageDbHelper(this.context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
