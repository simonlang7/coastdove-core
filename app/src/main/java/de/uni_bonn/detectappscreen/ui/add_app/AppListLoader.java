package de.uni_bonn.detectappscreen.ui.add_app;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.Loader;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.uni_bonn.detectappscreen.utility.CollatorWrapper;

/**
 * Loader for the list of installed apps on the device
 */
public class AppListLoader extends Loader<ArrayList<ApplicationInfo>> {

    /**
     * Creates a new AppListLoader
     * @param context           Application context
     */
    public AppListLoader(Context context) {
        super(context);
    }

    /**
     * Loads the list of installed apps on the device
     */
    @Override
    public void onStartLoading() {
        final PackageManager packageManager = getContext().getPackageManager();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        //List<ApplicationInfo> appInfos = packageManager.getInstalledApplications(0);

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

        deliverResult(data);
    }
}