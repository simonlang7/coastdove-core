/*  DetectAppScreen
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

package de.uni_bonn.detectappscreen.app_usage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.Loader;

import de.uni_bonn.detectappscreen.analyze.AppUsageDataProcessor;
import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageDbHelper;

/**
 * Loader for app usage data and an according processor
 */
public class AppUsageDataProcessorLoader extends Loader<AppUsageDataProcessor> {

    /** Package name of the app */
    private String appPackageName;
    /** Primary key of the app */
    private int appID;

    /**
     * Creates a new AppUsageDataProcessorLoader using the given data
     * @param context        Application context
     * @param appID          Primary key of the app
     */
    public AppUsageDataProcessorLoader(Context context, String appPackageName, int appID) {
        super(context);
        this.appPackageName = appPackageName;
        this.appID = appID;
    }

    /**
     * Loads the app usage data + processor for the given appID
     */
    @Override
    public void onStartLoading() {
        AppUsageDbHelper dbHelper = new AppUsageDbHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        AppUsageData appUsageData = AppUsageData.fromSQLiteDB(db, appPackageName, appID);

        AppUsageDataProcessor processor = new AppUsageDataProcessor(getContext(), this.appPackageName,
                appUsageData);

        dbHelper.close();
        deliverResult(processor);
    }
}