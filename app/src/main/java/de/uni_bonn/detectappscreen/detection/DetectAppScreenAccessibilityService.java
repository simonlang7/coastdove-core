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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.Map;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;
import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageDbHelper;
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
    /** Data for app detection */
    private Map<String, AppDetectionData> detectableApps;


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
     * Activates any detectable app that has finished loading
     */
    private void activateDetectableApps() {
        for (AppDetectionData data : appDetectionDataMultiLoader.getAll()) {
            if (!this.detectableApps.containsKey(data.getAppPackageName())) {
                Log.i("Loaded DetectableApp", data.getAppPackageName());
                this.detectableApps.put(data.getAppPackageName(), data);
            }
        }
    }

    /**
     * Checks the current activity and compares layouts of the current app as necessary
     * @param event
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null) {
            String packageName = event.getPackageName().toString();

            checkActivity(event);

            // Handle layout detection
            AppDetectionData detectionData = this.detectableApps.get(packageName);
            if (detectionData != null) {
                detectionData.performChecks(event, getRootInActiveWindow(), currentActivity);
            }

            // Changed app? Write gathered data to file
            // todo: separate function?
            int slashPos = currentActivity.indexOf('/');
            String activityPackageName = currentActivity.substring(0, slashPos < 0 ? 0 : slashPos);
            if (!activityPackageName.equals(previousPackageName)) {
                AppDetectionData previousDetectionData = this.detectableApps.get(previousPackageName);
                if (previousDetectionData != null)
                    previousDetectionData.saveAppUsageData();
            }

            // Changed activity?
            if (!activityPackageName.equals(previousPackageName))
                activateDetectableApps();

            this.previousPackageName = activityPackageName;
        }



//        Log.i("AccessibilityEvent", "" + event.getEventType());
    }

    // TODO: remove?
    private String idToText(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                return "TYPE_TOUCH_EXPLORATION_GESTURE_START";
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                return "TYPE_TOUCH_EXPLORATION_GESTURE_END";
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START:
                return "TYPE_TOUCH_INTERACTION_START";
            case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END:
                return "TYPE_TOUCH_INTERACTION_END";
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_START:
                return "TYPE_GESTURE_DETECTION_START";
            case AccessibilityEvent.TYPE_GESTURE_DETECTION_END:
                return "TYPE_GESTURE_DETECTION_END";
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                return "TYPE_VIEW_HOVER_ENTER";
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                return "TYPE_VIEW_HOVER_EXIT";
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                return "TYPE_VIEW_SCROLLED";
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                return "TYPE_VIEW_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                return "TYPE_VIEW_LONG_CLICKED";
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                return "TYPE_VIEW_FOCUSED";
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                return "TYPE_VIEW_SELECTED";
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED:
                return "TYPE_VIEW_ACCESSIBILITY_FOCUSED";
            case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED:
                return "TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED";
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                return "TYPE_WINDOW_STATE_CHANGED";
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                return "TYPE_NOTIFICATION_STATE_CHANGED";
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                return "TYPE_ANNOUNCEMENT";
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                return "TYPE_WINDOWS_CHANGED";
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                return "TYPE_WINDOW_CONTENT_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                return "TYPE_VIEW_TEXT_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                return "TYPE_VIEW_TEXT_SELECTION_CHANGED";
            case AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY:
                return "TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY";
        }
        return "Unknown";
    }

    /**
     * Initializes this accessibility service
     */
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //Configure these here for compatibility with API 13 and below.
        //AccessibilityServiceInfo config = new AccessibilityServiceInfo();
//        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
//        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        //config.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        //config.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        //config.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        //config.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;

//        if (Build.VERSION.SDK_INT >= 16)
//            //Just in case this helps
//            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        //setServiceInfo(config);

//        AppUsageDbHelper dbHelper = new AppUsageDbHelper(getApplicationContext());
//        SQLiteDatabase db = dbHelper.getReadableDatabase();
//        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
//        if (c.moveToFirst()) {
//            while (!c.isAfterLast()) {
//                Log.d("Table", c.getString(0));
//                if (c.getString(0).equals("android_metadata")) {
//                    c.moveToNext();
//                    continue;
//                }
//                Cursor u = db.rawQuery("SELECT * FROM " + c.getString(0), null);
//                if (u.moveToFirst()) {
//                    String cols = "";
//                    while (!u.isAfterLast()) {
//                        cols += u.getString(0) + " ";
//                    }
//                    Log.d("Columns", cols);
//                }
//                c.moveToNext();
//            }
//        }
//        dbHelper.close();
        

        this.currentActivity = "";
        this.previousPackageName = "";

        this.detectableApps = new HashMap<>();
        activateDetectableApps();
    }

    @Override
    public void onInterrupt() {

    }
}
