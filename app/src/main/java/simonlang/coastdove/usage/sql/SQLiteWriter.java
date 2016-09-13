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

package simonlang.coastdove.usage.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import simonlang.coastdove.usage.AppUsageData;

/**
 * Writes an AppUsageData object into the SQLite database
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
            db.close();
        }
    }
}
