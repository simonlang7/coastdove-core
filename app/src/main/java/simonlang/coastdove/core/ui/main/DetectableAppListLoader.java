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

package simonlang.coastdove.core.ui.main;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;

import simonlang.coastdove.core.utility.FileHelper;
import simonlang.coastdove.lib.CollatorWrapper;

/**
 * Loader for a list of detectable apps (with an AppDetectionData.bin stored on the device)
 */
public class DetectableAppListLoader extends AsyncTaskLoader<ArrayList<ApplicationInfo>> {

    /**
     * Creates a new DetectableAppListLoader
     * @param context           Application context
     */
    public DetectableAppListLoader(Context context) {
        super(context);
    }

    @Override
    public ArrayList<ApplicationInfo> loadInBackground() {
        File directory = FileHelper.getFile(getContext(), FileHelper.Directory.PRIVATE, null, "");
        String[] apps = directory.exists() ? directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return FileHelper.appDetectionDataExists(getContext(), filename);
            }
        }) : new String[0];

        ArrayList<String> dataAsStrings = new ArrayList<>(apps.length);
        for (String app : apps)
            dataAsStrings.add(app);
        Collections.sort(dataAsStrings, new CollatorWrapper());
        
        ArrayList<ApplicationInfo> data = new ArrayList<>(apps.length);
        for (String app : dataAsStrings) {
            PackageManager packageManager = getContext().getPackageManager();
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(app, 0);
                data.add(appInfo);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("DetectableAppListLoader", "Unable to find app " + app + ": " + e.getMessage());
            }
        }
        return data;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }
}