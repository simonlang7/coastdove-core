package de.uni_bonn.detectappscreen;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

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
 * TODO: the items clicked by the user, the navigation buttons (home/back) clicked by the user
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
     * @param packageName The package name of the app to load detection data for
     * @param context     The application context
     */
    public static void startLoadingDetectionData(String packageName, Context context) {
        // Already loaded?
        synchronized (detectableAppsLoadedLock) {
            for (AppDetectionData data : detectableAppsLoaded) {
                if (data.getPackageName().equals(packageName))
                    return;
            }
        }
        // Already loading?
        if (detectableAppsLoading.containsKey(packageName))
            return;

        // Start new thread
        AppDetectionDataLoader loader = new AppDetectionDataLoader(packageName, detectableAppsLoaded, context);
        Thread loaderThread = new Thread(loader);
        detectableAppsLoading.put(packageName, loaderThread);
        loaderThread.start();
        Log.i("AppDetectionData", "Loading " + packageName);
    }

    /**
     * Removes detection data for the given app, or stops loading the data if loading
     * is currently in progress
     * @param packageName
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
                if (data.getPackageName().equals(packageName)) {
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
        if (detectableAppsLoading.containsKey(packageName))
            return true;
        synchronized (detectableAppsLoadedLock) {
            for (AppDetectionData data : detectableAppsLoaded) {
                if (data.getPackageName().equals(packageName))
                    return true;
            }
        }

        return false;
    }

    /**
     * Removes loader thread from {@link DetectAppScreenAccessibilityService#detectableAppsLoading}
     * @param packageName Name of the package for the associated loader thread
     */
    public static void onDetectionDataLoadFinished(String packageName) {
        if (detectableAppsLoading.containsKey(packageName)) {
            detectableAppsLoading.remove(packageName);
            Log.i("AppDetectionData", "Removed LoaderThread");
        }
    }


    /** Name of the currenty activity, as acquired during the last window state change event */
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
                this.currentActivity = componentName.flattenToShortString();
                Log.i("CurrentActivity", componentName.flattenToShortString());
            }
        }
    }

    /**
     * Returns true iff a layout comparison shall be performed
     */
    private boolean shallPerformLayoutComparison(AccessibilityEvent event) {
        // TODO: only on window state changed or window content changed
        AccessibilityNodeInfo source = event.getSource();
        return source != null &&
                (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
                //&& System.currentTimeMillis() - timeOfLastLayoutComparison >= TIME_BETWEEN_LAYOUT_COMPARISONS;
    }

    /**
     * Activates any detectable app that has finished loading
     */
    private void activateDetectableApps() {
        synchronized (detectableAppsLoadedLock) {
            for (AppDetectionData data : detectableAppsLoaded) {
                if (!this.detectableApps.containsKey(data.getPackageName())) {
                    Log.i("Loaded DetectableApp", data.getPackageName());
                    this.detectableApps.put(data.getPackageName(), data);
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
                if (shallPerformLayoutComparison(event)) {
                    detectionData.checkLayout(event, currentActivity);
                    timeOfLastLayoutComparison = System.currentTimeMillis();
                }
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

            // Clicked somewhere?
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                AccessibilityNodeInfo source = event.getSource();
                if (source != null) {
                    Log.i("CLICKED (id)", source.getViewIdResourceName() != null ? source.getViewIdResourceName() : "-");
                    Log.i("CLICKED (descr)", source.getContentDescription() != null ? source.getContentDescription().toString() : "-");
                    Log.i("CLICKED (text)", source.getText() != null ? source.getText().toString() : "-");
                }
            }

            // Changed activity?
            if (!activityPackageName.equals(previousPackageName))
                activateDetectableApps();

            this.previousPackageName = activityPackageName;
        }



//        Log.i("AccessibilityEvent", "" + event.getEventType());
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
