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

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import simonlang.coastdove.app_usage.NotificationEvent;
import simonlang.coastdove.app_usage.sql.AppUsageDbHelper;
import simonlang.coastdove.utility.MultipleObjectLoader;

/**
 * Accessibility Service for [co]llecting [a]pp usage [st]atistics. Keeps a map of AppDetectionData
 * globally and uses these to collect usage data when using any Android app, identified by its package name.
 */
public class CoastAccessibilityService extends AccessibilityService {

    /** Contains all AppDetectionData needed to process detectable apps */
    protected static MultipleObjectLoader<AppDetectionData> appDetectionDataMultiLoader = new MultipleObjectLoader<>();

    /**
     * Returns the MultipleObjectLoader used to load AppDetectionData
     */
    public static MultipleObjectLoader<AppDetectionData> getAppDetectionDataMultiLoader() {
        return appDetectionDataMultiLoader;
    }

    /** Receiver for when the screen turns off or on */
    private ScreenStateReceiver screenStateReceiver;
    /** Name of the current activity, as acquired during the last window state change event */
    private String currentActivity;
    /** Name of the previous app, as extracted from the last activity of the previous app */
    private String previousPackageName;


    /**
     * Checks the current activity and compares layouts of the current app as necessary
     * @param event    Accessibility Event that triggered this method
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null) {
            String packageName = event.getPackageName().toString();

            checkActivity(event);

            // Handle layout detection
            AppDetectionData detectionData = appDetectionDataMultiLoader.get(packageName);
            if (detectionData != null) {
                screenStateReceiver.setCurrentDetectionData(detectionData);
                detectionData.performChecks(event, getRootInActiveWindow(), currentActivity);
            }

            // Changed app? Write gathered data to file
            checkPackageChanged();
        }
    }

    /**
     * Returns the activity info of the current activity
     */
    private ActivityInfo getCurrentActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Stores the name of the current activity in {@link CoastAccessibilityService#currentActivity}
     * @param event    AccessibilityEvent that has occurred
     */
    private void checkActivity(AccessibilityEvent event) {
        // New activity?
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            ActivityInfo activityInfo = getCurrentActivity(componentName);
            if (activityInfo != null) {
                String newActivity = componentName.flattenToShortString();
                this.currentActivity = newActivity;
                Log.d("CurrentActivity", newActivity);
            }
        }
    }

    private void checkPackageChanged() {
        int slashPos = currentActivity.indexOf('/');
        String activityPackageName = currentActivity.substring(0, slashPos < 0 ? 0 : slashPos);
        if (!activityPackageName.equals(previousPackageName)) {
            AppDetectionData previousDetectionData = appDetectionDataMultiLoader.get(previousPackageName);
            if (previousDetectionData != null) {
                screenStateReceiver.setCurrentDetectionData(null);
                previousDetectionData.saveAppUsageData();
            }
        }
        this.previousPackageName = activityPackageName;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        this.screenStateReceiver = new ScreenStateReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(this.screenStateReceiver, filter);

        this.currentActivity = "";
        this.previousPackageName = "";
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(this.screenStateReceiver);

        super.onDestroy();
    }

    @Override
    public void onInterrupt() {

    }
}
