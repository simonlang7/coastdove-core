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

package de.uni_bonn.detectappscreen.ui.main;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;
import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageDbHelper;
import de.uni_bonn.detectappscreen.utility.CollatorWrapper;
import de.uni_bonn.detectappscreen.utility.FileHelper;

/**
 * Loader for a list of files in a directory
 */
public class DetectableAppListLoader extends AsyncTaskLoader<ArrayList<String>> {

    /**
     * Creates a new DetectableAppListLoader
     * @param context           Application context
     */
    public DetectableAppListLoader(Context context) {
        super(context);
    }

    @Override
    public ArrayList<String> loadInBackground() {
        File directory = FileHelper.getFile(getContext(), FileHelper.Directory.PUBLIC, null, "");
        String[] files = directory.exists() ? directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                boolean detectableDataExists = FileHelper.appDetectionDataExists(getContext(), filename);

                if (detectableDataExists)
                    return true;

                AppUsageDbHelper dbHelper = new AppUsageDbHelper(getContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                String[] projection = { AppUsageContract.AppTable.COLUMN_NAME_PACKAGE };
                String selection = AppUsageContract.AppTable.COLUMN_NAME_PACKAGE + "=?";
                String[] selectionArgs = { filename };

                Cursor c = db.query(AppUsageContract.AppTable.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, null);
                boolean usageDataExists = c.moveToFirst();
                c.close();
                dbHelper.close();

                return usageDataExists;
            }
        }) : new String[0];
        
        ArrayList<String> data = new ArrayList<>(files.length);
        for (String file : files)
            data.add(file);
        Collections.sort(data, new CollatorWrapper());
        return data;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }
}