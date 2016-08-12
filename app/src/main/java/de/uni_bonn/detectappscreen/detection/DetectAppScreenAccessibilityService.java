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
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ProgressBar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accessibility Service for app screen detection. Once started, data needed for app detection
 * are loaded and the service initializes. The service collects the following data: the current
 * activity, the detected layouts of the current app (if the according data was loaded beforehand),
 * and the items clicked by the user
 */
public class DetectAppScreenAccessibilityService extends AccessibilityService {

    /** Time (in milliseconds) between two layout comparisons */
    protected static final int TIME_BETWEEN_LAYOUT_COMPARISONS = 100;
    /** Threads that are currently loading app detection data */
    protected static Map<String, Thread> detectableAppsLoading = new ConcurrentHashMap<>();
    /** App detection data that has finished loading. Shall only be accessed in a synchronized
     * block locking {@link DetectAppScreenAccessibilityService#detectableAppsLoadedLock} */
    protected static List<AppDetectionData> detectableAppsLoaded = new LinkedList<>(); // todo: replace by concurrent hashmap?
    /** Monitor for any synchronized block accessing {@link DetectAppScreenAccessibilityService#detectableAppsLoaded} */
    public static final Object detectableAppsLoadedLock = new Object();


    /**
     * Starts loading detection data for the given app. Loading is done in a new thread.
     * @param appPackageName The package name of the app to load detection data for
     * @param context     The application context
     */
    public static void startLoadingDetectionData(String appPackageName, boolean performLayoutChecks,
                                                 boolean performOnClickChecks, Context context) {
        startLoadingDetectionData(appPackageName, performLayoutChecks, performOnClickChecks, context, null);
    }

    /**
     * Starts loading detection data for the given app. Loading is done in a new thread.
     * @param appPackageName The package name of the app to load detection data for
     * @param context     The application context
     */
    public static void startLoadingDetectionData(String appPackageName, boolean performLayoutChecks,
                                                 boolean performOnClickChecks, Context context,
                                                 ProgressBar progressBar) {
        // Already loaded?
        synchronized (detectableAppsLoadedLock) {
            for (AppDetectionData data : detectableAppsLoaded) {
                if (data.getAppPackageName().equals(appPackageName))
                    return;
            }
        }
        // Already loading?
        if (detectableAppsLoading.containsKey(appPackageName))
            return;

        // Start new thread
        AppDetectionDataLoader loader = new AppDetectionDataLoader(appPackageName, detectableAppsLoaded,
                performLayoutChecks, performOnClickChecks, context);
        if (progressBar != null)
            loader.setProgressBar(progressBar);
        Thread loaderThread = new Thread(loader);
        detectableAppsLoading.put(appPackageName, loaderThread);
        loaderThread.start();
        Log.i("AppDetectionData", "Loading " + appPackageName);
    }

    /**
     * Removes detection data for the given app / package name, or stops loading the data if loading
     * is currently in progress
     */
    public static void removeDetectionData(String packageName) {
        // Stop loading, remove from loading threads
        if (detectableAppsLoading.containsKey(packageName)) {
            Thread loaderThread = detectableAppsLoading.get(packageName);
            loaderThread.interrupt();
            Log.i("AppDetectionData", "Stopped LoaderThread");
            detectableAppsLoading.remove(packageName);
            Log.i("AppDetectionData", "Removed LoaderThread");
        }

        // Remove from already loaded data
        synchronized (detectableAppsLoadedLock) {
            Iterator<AppDetectionData> iterator = detectableAppsLoaded.iterator();
            while (iterator.hasNext()) {
                AppDetectionData data = iterator.next();
                if (data.getAppPackageName().equals(packageName)) {
                    iterator.remove();
                    Log.i("AppDetectionData", "Removed loaded data");
                    break;
                }
            }
        }
    }

    /**
     * Tells whether or not detection data for the given app is currently being
     * loaded or finished loading
     * @param packageName    Package name of the app in question
     * @return true iff detection data for the given app loaded or in the process of being loaded
     */
    public static boolean isDetectionDataLoadedOrLoading(String packageName) {
        if (detectableAppsLoading != null && detectableAppsLoading.containsKey(packageName))
            return true;
        if (detectableAppsLoaded != null) {
            synchronized (detectableAppsLoadedLock) {
                for (AppDetectionData data : detectableAppsLoaded) {
                    if (data.getAppPackageName().equals(packageName))
                        return true;
                }
            }
        }

        return false;
    }

    /**
     * Removes loader thread from {@link DetectAppScreenAccessibilityService#detectableAppsLoading}
     * @param packageName Name of the package for the associated loader thread
     */
    public static void onDetectionDataLoadFinished(String packageName) {
        if (detectableAppsLoading != null && detectableAppsLoading.containsKey(packageName)) {
            detectableAppsLoading.remove(packageName);
            Log.i("AppDetectionData", "Removed LoaderThread");
        }
    }


    /** Name of the current activity, as acquired during the last window state change event */
    private String currentActivity;
    /** Name of the previous app, as extracted from the last activity of the previous app */
    private String previousPackageName;
    /** Time (in milliseconds) passed since the last layout comparison */
    private long timeOfLastLayoutComparison = 0;
    /** Data for app detection */
    private Map<String, AppDetectionData> detectableApps;


    /**
     * Returns the activity info of the current activity
     */
    private ActivityInfo tryGetActivity(ComponentName componentName) {
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

            ActivityInfo activityInfo = tryGetActivity(componentName);
            if (activityInfo != null) {
                String newActivity = componentName.flattenToShortString();
                this.currentActivity = newActivity;
                Log.i("CurrentActivity", newActivity);
            }
        }
    }

    /**
     * Activates any detectable app that has finished loading
     */
    private void activateDetectableApps() {
        synchronized (detectableAppsLoadedLock) {
            for (AppDetectionData data : detectableAppsLoaded) {
                if (!this.detectableApps.containsKey(data.getAppPackageName())) {
                    Log.i("Loaded DetectableApp", data.getAppPackageName());
                    this.detectableApps.put(data.getAppPackageName(), data);
                }
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
                timeOfLastLayoutComparison = System.currentTimeMillis();
            }

            // Changed app? Write gathered data to file
            // todo: separate function
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

        this.currentActivity = "";
        this.previousPackageName = "";

        this.detectableApps = new HashMap<>();
        activateDetectableApps();
    }

    @Override
    public void onInterrupt() {

    }
}
