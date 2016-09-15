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

package simonlang.coastdove.core.ui.add_app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Loader for the list of installed apps on the device
 */
public class AppListLoader extends AsyncTaskLoader<ArrayList<ApplicationInfo>> {

    /**
     * Creates a new AppListLoader
     * @param context           Application context
     */
    public AppListLoader(Context context) {
        super(context);
    }

    @Override
    public ArrayList<ApplicationInfo> loadInBackground() {
        final PackageManager packageManager = getContext().getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);

        ArrayList<ApplicationInfo> data = new ArrayList<>();
        for (int i = 0; i < packageInfos.size(); ++i) {
            PackageInfo packageInfo = packageInfos.get(i);
            data.add(packageInfo.applicationInfo);
        }

        Collections.sort(data, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                String lhsAppName = lhs.loadLabel(packageManager).toString();
                String rhsAppName = rhs.loadLabel(packageManager).toString();
                return lhsAppName.compareTo(rhsAppName);
            }
        });

        return data;
    }

    /**
     * Loads the list of installed apps on the device
     */
    @Override
    public void onStartLoading() {
        forceLoad();
//        deliverResult(data);
    }
}