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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper to access SQLite database for app usage data
 */
public class AppUsageDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 8;
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
        db.execSQL(AppUsageContract.SQL_CREATE_SCREEN_OFF_DETAILS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(AppUsageContract.SQL_DELETE_APPS);
        db.execSQL(AppUsageContract.SQL_DELETE_ACTIVITIES);
        db.execSQL(AppUsageContract.SQL_DELETE_DATA_ENTRIES);
        db.execSQL(AppUsageContract.SQL_DELETE_INTERACTION_DETAILS);
        db.execSQL(AppUsageContract.SQL_DELETE_LAYOUT_DETAILS);
        db.execSQL(AppUsageContract.SQL_DELETE_SCREEN_OFF_DETAILS);
        db.execSQL(AppUsageContract.SQL_DELETE_SCROLL_DETAILS);
        onCreate(db);
    }
}