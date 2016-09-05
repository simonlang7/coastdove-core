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

import android.content.Context;

import java.io.File;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.setup.AppDetectionDataSetup;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.utility.FileHelper;
import de.uni_bonn.detectappscreen.utility.Misc;
import de.uni_bonn.detectappscreen.utility.MultipleObjectLoader;
import de.uni_bonn.detectappscreen.utility.ObjectLoader;

/**
 * Loader for app detection data that is supposed to be run in a separate thread
 */
public class AppDetectionDataLoader extends ObjectLoader<AppDetectionData> {

    /** Package name associated with the app detection data */
    private String appPackageName;
    /** Whether to compare current layouts with layout definitions */
    private boolean performLayoutChecks;
    /** Whether to listen to interaction events */
    private boolean performInteractionChecks;
    /** Full path to the APK file */
    private String fullApkPath;

    /** Application context */
    private Context context;

    /** UI elements to display loading progress */
    private LoadingInfo loadingInfo;

    /**
     * Constructs a loader using the given data
     * @param appPackageName          Name of the package associated
     */
    public AppDetectionDataLoader(String appPackageName, MultipleObjectLoader<AppDetectionData> multipleObjectLoader,
                                  boolean performLayoutChecks, boolean performInteractionChecks, Context context,
                                  LoadingInfo loadingInfo) {
        super(appPackageName, multipleObjectLoader);
        this.appPackageName = appPackageName;
        this.performLayoutChecks = performLayoutChecks;
        this.performInteractionChecks = performInteractionChecks;
        this.context = context;
        this.loadingInfo = loadingInfo;
        this.fullApkPath = null;
    }

    public AppDetectionDataLoader(String appPackageName, MultipleObjectLoader<AppDetectionData> multipleObjectLoader,
                                  String fullApkPath, Context context, LoadingInfo loadingInfo) {
        super(appPackageName, multipleObjectLoader);
        this.appPackageName = appPackageName;
        this.performLayoutChecks = Misc.DEFAULT_DETECT_LAYOUTS;
        this.performInteractionChecks = Misc.DEFAULT_DETECT_INTERACTIONS;
        this.context = context;
        this.loadingInfo = loadingInfo;
        this.fullApkPath = fullApkPath;
    }

    /**
     * Loads the according app detection data
     */
    @Override
    protected AppDetectionData load() {
        AppDetectionData detectableApp;
        if (this.fullApkPath != null) {
            File apkFile = new File(this.fullApkPath);
            detectableApp = AppDetectionDataSetup.fromAPK(this.context, apkFile, appPackageName, 1.0f, loadingInfo);
            FileHelper.writeAppDetectionData(this.context, detectableApp, FileHelper.Directory.PRIVATE_PACKAGE, this.appPackageName, "AppDetectionData.bin");
        }
        else {
            loadingInfo.setNotificationData(context.getString(R.string.add_app_notification_loading),
                    appPackageName, R.drawable.notification_template_icon_bg);
            loadingInfo.start(true);

            detectableApp = FileHelper.readAppDetectionData(this.context, FileHelper.Directory.PRIVATE_PACKAGE, this.appPackageName, "AppDetectionData.bin");

            loadingInfo.setNotificationData(context.getString(R.string.add_app_notification_finished_loading),
                    null, null);
            loadingInfo.end();
        }

        detectableApp.init(this.performLayoutChecks, this.performInteractionChecks, this.context);
        return detectableApp;
    }
}
