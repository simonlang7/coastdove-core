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

package simonlang.coastdove.detection;

import android.content.Context;
import android.util.Log;

import java.io.File;

import simonlang.coastdove.setup.AppDetectionDataSetup;
import simonlang.coastdove.ui.LoadingInfo;
import simonlang.coastdove.utility.FileHelper;
import simonlang.coastdove.utility.Misc;
import simonlang.coastdove.utility.MultipleObjectLoader;
import simonlang.coastdove.utility.ObjectLoader;

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
    /** Whether to react to screen state changes */
    private boolean performScreenStateChecks;
    /** Whether to check notifications */
    private boolean performNotificationChecks;
    /** Full path to the APK file */
    private String fullApkPath;
    /** Whether to load replacement data */
    private boolean loadReplacementData;

    /** Application context */
    private Context context;

    /** UI elements to display loading progress */
    private LoadingInfo loadingInfo;

    /**
     * Constructs a loader using the given data
     * @param appPackageName          Name of the package associated
     */
    public AppDetectionDataLoader(String appPackageName, MultipleObjectLoader<AppDetectionData> multipleObjectLoader,
                                  boolean performLayoutChecks, boolean performInteractionChecks, boolean performScreenStateChecks,
                                  boolean loadReplacementData, boolean performNotificationChecks, Context context, LoadingInfo loadingInfo) {
        super(appPackageName, multipleObjectLoader);
        this.appPackageName = appPackageName;
        this.performLayoutChecks = performLayoutChecks;
        this.performInteractionChecks = performInteractionChecks;
        this.performScreenStateChecks = performScreenStateChecks;
        this.performNotificationChecks = performNotificationChecks;
        this.loadReplacementData = loadReplacementData;
        this.context = context;
        this.loadingInfo = loadingInfo;
        this.fullApkPath = null;
    }

    public AppDetectionDataLoader(String appPackageName, MultipleObjectLoader<AppDetectionData> multipleObjectLoader,
                                  String fullApkPath, boolean loadReplacementData, Context context, LoadingInfo loadingInfo) {
        super(appPackageName, multipleObjectLoader);
        this.appPackageName = appPackageName;
        this.performLayoutChecks = Misc.DEFAULT_DETECT_LAYOUTS;
        this.performInteractionChecks = Misc.DEFAULT_DETECT_INTERACTIONS;
        this.performScreenStateChecks = Misc.DEFAULT_DETECT_SCREEN_STATE;
        this.performNotificationChecks = Misc.DEFAULT_DETECT_NOTIFICATIONS;
        this.loadReplacementData = loadReplacementData;
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
            // Create detection data from APK file
            File apkFile = new File(this.fullApkPath);
            detectableApp = AppDetectionDataSetup.fromAPK(this.context, apkFile, appPackageName, 1.0f, loadingInfo);
            if (detectableApp == null)
                return null;
            FileHelper.writeAppDetectionData(this.context, detectableApp, FileHelper.Directory.PRIVATE_PACKAGE, this.appPackageName, FileHelper.APP_DETECTION_DATA_FILENAME);
        }
        else {
            // Load cache
            detectableApp = FileHelper.readAppDetectionData(this.context, FileHelper.Directory.PRIVATE_PACKAGE, this.appPackageName, FileHelper.APP_DETECTION_DATA_FILENAME);
        }

        // Load replacement data
        ReplacementData replacementData = null;
        if (this.loadReplacementData)
            replacementData = Misc.loadReplacementData(context, appPackageName);

        // Initialize
        detectableApp.init(this.performLayoutChecks, this.performInteractionChecks, this.performScreenStateChecks,
                this.performNotificationChecks, replacementData, this.context);
        // TODO: re-work LoadingInfo with callbacks in activity, update activity to show appropriate bar
        Log.d("AppDetectionDataLoader", "Accuracy: " + detectableApp.getAccuracy() + "%");
        return detectableApp;
    }
}
