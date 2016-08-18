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

package de.uni_bonn.detectappscreen.detection;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v7.app.NotificationCompat;
import android.widget.ProgressBar;

import org.json.JSONObject;

import java.util.List;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.utility.FileHelper;

/**
 * Loader for app detection data that is supposed to be run in a separate thread
 */
public class AppDetectionDataLoader implements Runnable {

    /** Package name associated with the app detection data */
    private String appPackageName;
    /** List to add the loaded object to */
    private List<AppDetectionData> detectableAppsLoaded;
    /** Whether to compare current layouts with layout definitions */
    private boolean performLayoutChecks;
    /** Whether to listen to OnClick events */
    private boolean performOnClickChecks;

    /** (Optional) progress bar to display the loading progress */
    private ProgressBar progressBar;

    /** Application context */
    private Context context;

    /** UI elements to display loading progress */
    private LoadingInfo loadingInfo;

    /**
     * Constructs a loader using the given data
     * @param appPackageName          Name of the package associated
     * @param detectableAppsLoaded    List to add the loaded object to
     */
    public AppDetectionDataLoader(String appPackageName, List<AppDetectionData> detectableAppsLoaded,
                                  boolean performLayoutChecks, boolean performOnClickChecks,
                                  Context context, LoadingInfo loadingInfo) {
        super();
        this.appPackageName = appPackageName;
        this.detectableAppsLoaded = detectableAppsLoaded;
        this.performLayoutChecks = performLayoutChecks;
        this.performOnClickChecks = performOnClickChecks;
        this.context = context;
        this.loadingInfo = loadingInfo;
    }

    /**
     * Loads the according app detection data
     */
    @Override
    public void run() {
        JSONObject layouts;
        JSONObject reverseMap;
        synchronized (DetectAppScreenAccessibilityService.detectableAppsLoadedLock) {
            layouts = FileHelper.readJSONFile(
                    this.context, FileHelper.Directory.PACKAGE, this.appPackageName, "layouts.json");
            reverseMap = FileHelper.readJSONFile(
                    this.context, FileHelper.Directory.PACKAGE, this.appPackageName, "reverseMap.json");
        }
        AppDetectionData detectableApp = new AppDetectionData(this.appPackageName, layouts, reverseMap, context);
        if (this.loadingInfo != null)
            this.loadingInfo.uid = detectableApp.getUid();

        detectableApp.setHashMapLoadingInfo(this.loadingInfo);

        detectableApp.load(this.performLayoutChecks, this.performOnClickChecks);
        synchronized (DetectAppScreenAccessibilityService.detectableAppsLoadedLock) {
            if (detectableApp.isFinishedLoading())
                detectableAppsLoaded.add(detectableApp);
        }
        DetectAppScreenAccessibilityService.onDetectionDataLoadFinished(this.appPackageName);
    }

    /** (Optional) progress bar to display the loading progress */
    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }
}
