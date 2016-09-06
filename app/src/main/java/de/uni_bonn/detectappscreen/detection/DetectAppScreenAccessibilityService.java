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

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import de.uni_bonn.detectappscreen.utility.MultipleObjectLoader;

/**
 * Accessibility Service for app screen detection. Once started, data needed for app detection
 * are loaded and the service initializes. The service collects the following data: the current
 * activity, the detected layouts of the current app (if the according data was loaded beforehand),
 * and the items clicked by the user
 */
public class DetectAppScreenAccessibilityService extends AccessibilityService {

    /** Contains all AppDetectionData needed to process detectable apps */
    protected static MultipleObjectLoader<AppDetectionData> appDetectionDataMultiLoader = new MultipleObjectLoader<>();

    /**
     * Returns the MultipleObjectLoader used to load AppDetectionData
     */
    public static MultipleObjectLoader<AppDetectionData> getAppDetectionDataMultiLoader() {
        return appDetectionDataMultiLoader;
    }


    /** Name of the current activity, as acquired during the last window state change event */
    private String currentActivity;
    /** Name of the previous app, as extracted from the last activity of the previous app */
    private String previousPackageName;


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
     * Stores the name of the current activity in {@link DetectAppScreenAccessibilityService#currentActivity}
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
            if (detectionData != null)
                detectionData.performChecks(event, getRootInActiveWindow(), currentActivity);

            // Changed app? Write gathered data to file
            checkPackageChanged();
        }
    }

    private void checkPackageChanged() {
        int slashPos = currentActivity.indexOf('/');
        String activityPackageName = currentActivity.substring(0, slashPos < 0 ? 0 : slashPos);
        if (!activityPackageName.equals(previousPackageName)) {
            AppDetectionData previousDetectionData = appDetectionDataMultiLoader.get(previousPackageName);
            if (previousDetectionData != null)
                previousDetectionData.saveAppUsageData();
        }
        this.previousPackageName = activityPackageName;
    }

    /**
     * Initializes this accessibility service
     */
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        this.currentActivity = "";
        this.previousPackageName = "";
    }

    @Override
    public void onInterrupt() {

    }
}
