package de.uni_bonn.detectappscreen;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
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

/**
 * Created by simon on 20/01/16.
 */
public class DetectAppScreenAccessibilityService extends AccessibilityService {

    protected static final int TIME_BETWEEN_LAYOUT_COMPARISONS = 100;
    protected static Map<String, Thread> detectableAppsLoading = new HashMap<>();
    protected static List<AppDetectionData> detectableAppsLoaded = new LinkedList<>();
    public static final Object detectableAppsLoadedLock = new Object();

    public static void startLoadingDetectionData(String packageName) {
        synchronized (detectableAppsLoadedLock) {
            for (AppDetectionData data : detectableAppsLoaded) {
                if (data.getPackageName().equals(packageName))
                    return;
            }
        }
        if (detectableAppsLoading.containsKey(packageName))
            return;

        AppDetectionDataLoader loader = new AppDetectionDataLoader(packageName, detectableAppsLoaded);
        Thread loaderThread = new Thread(loader);
        detectableAppsLoading.put(packageName, loaderThread);
        loaderThread.start();
        Log.i("AppDetectionData", "Loading " + packageName);
    }

    public static void removeDetectionData(String packageName) {
        if (detectableAppsLoading.containsKey(packageName)) {
            Thread loaderThread = detectableAppsLoading.get(packageName);
            loaderThread.interrupt();
            Log.i("AppDetectionData", "Stopped LoaderThread");
            detectableAppsLoading.remove(packageName);
            Log.i("AppDetectionData", "Removed LoaderThread");
        }

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

    public static void onDetectionDataLoadFinished(String packageName) {
        if (detectableAppsLoading.containsKey(packageName)) {
            detectableAppsLoading.remove(packageName);
            Log.i("AppDetectionData", "Removed LoaderThread");
        }
    }



    protected long timeOfLastLayoutComparison = 0;
    protected Map<String, AppDetectionData> detectableApps;


    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    protected boolean shallPerformLayoutComparison(AccessibilityNodeInfo source) {
        // TODO: only on window state changed or window content changed
        return source != null
                && System.currentTimeMillis() - timeOfLastLayoutComparison >= TIME_BETWEEN_LAYOUT_COMPARISONS;
    }

    protected void checkActivity(AccessibilityEvent event) {
        // New activity?
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            ActivityInfo activityInfo = tryGetActivity(componentName);
            boolean isActivity = activityInfo != null;
            if (isActivity)
                Log.i("CurrentActivity", componentName.flattenToShortString());
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() != null) {

            checkActivity(event);

            AccessibilityNodeInfo sourceNodeInfo = event.getSource();
            if (shallPerformLayoutComparison(sourceNodeInfo)) {
                String packageName = event.getPackageName().toString();
                AppDetectionData detectionData = this.detectableApps.get(packageName);
                if (detectionData != null)
                    detectionData.checkLayout(event);

                timeOfLastLayoutComparison = System.currentTimeMillis();
            }
        }
//        Log.i("AccessibilityEvent", "" + event.getEventType());
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //Configure these here for compatibility with API 13 and below.
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
//        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
//        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        config.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        config.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        //config.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;

//        if (Build.VERSION.SDK_INT >= 16)
//            //Just in case this helps
//            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);

        this.detectableApps = new HashMap<>();
        synchronized (detectableAppsLoaded) {
            for (AppDetectionData data : detectableAppsLoaded) {
                Log.i("Loaded DetectableApp", data.getPackageName());
                this.detectableApps.put(data.getPackageName(), data);
            }
        }
    }

    @Override
    public void onInterrupt() {

    }
}
