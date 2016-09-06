package de.uni_bonn.detectappscreen.app_usage.sql;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper to access SQLite database for app usage data
 */
public class AppUsageDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 7;
    public static final String DATABASE_NAME = "AppUsageData.db";


    public AppUsageDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(AppUsageContract.SQL_CREATE_APPS);
        db.execSQL(AppUsageContract.SQL_CREATE_ACTIVITIES);
        db.execSQL(AppUsageContract.SQL_CREATE_DATA_ENTRIES);
        db.execSQL(AppUsageContract.SQL_CREATE_INTERACTION_DETAILS);
        db.execSQL(AppUsageContract.SQL_CREATE_LAYOUT_DETAILS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(AppUsageContract.SQL_DELETE_APPS);
        db.execSQL(AppUsageContract.SQL_DELETE_ACTIVITIES);
        db.execSQL(AppUsageContract.SQL_DELETE_DATA_ENTRIES);
        db.execSQL(AppUsageContract.SQL_DELETE_INTERACTION_DETAILS);
        db.execSQL(AppUsageContract.SQL_DELETE_LAYOUT_DETAILS);
        db.execSQL(AppUsageContract.SQL_DELETE_SCROLL_DETAILS);
        onCreate(db);
    }
}