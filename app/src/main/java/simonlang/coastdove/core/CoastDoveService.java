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


    This framework is based on an idea by Rainer Duppré and Sergej Dechand,
    who implemented a study platform using Android's Accessibility Services.
*/

package simonlang.coastdove.core;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashMap;
import java.util.Map;

import simonlang.coastdove.core.detection.AppDetectionData;
import simonlang.coastdove.core.detection.ScreenStateReceiver;
import simonlang.coastdove.core.ipc.ListenerConnection;
import simonlang.coastdove.core.utility.Misc;
import simonlang.coastdove.core.utility.MultipleObjectLoader;

/**
 * Accessibility Service for [co]llecting [a]pp usage [st]atistics and [d]isplaying [ove]rlays,
 * but also other things.
 * Keeps a map of AppDetectionData globally and uses these to detect layouts and interaction
 * elements of arbitrary apps, identified by their package names. Notifies any listeners of changes
 * regarding such.
 */
public class CoastDoveService extends AccessibilityService {

    /** Contains all AppDetectionData needed to process detectable apps */
    public static final MultipleObjectLoader<AppDetectionData> multiLoader = new MultipleObjectLoader<>();
    /** All listeners of Coast Dove modules, i.e., connections to services in other apps listening to
     *  app detection performed here. Identified by the remote services' class names */
    public static final Map<String, ListenerConnection> listeners = new HashMap<>();

    /** The accessibility service itself, needed to access AccessibilityNodeInfos from
     * outside */
    private static CoastDoveService service = null;
    /** The accessibility service itself, needed to access AccessibilityNodeInfos from
     * outside */
    public static CoastDoveService getService() {
        return service;
    }


    /**
     * Adds a listener for the provided app to be enabled. If necessary, the listener is constructed,
     * otherwise the app is enabled on the existing listener.
     * @param context               App context
     * @param servicePackageName    Package name of the remote service
     * @param serviceClassName      Full class name (including all packages) of the remote service
     * @param appToEnable           App to listen to
     */
    public static void addListenerForApp(Context context, String servicePackageName, String serviceClassName,
                                         String appToEnable) {
        if (listeners.containsKey(serviceClassName)) {
            // Listener already running? Just enable the app
            listeners.get(serviceClassName).enableApp(appToEnable);
        }
        else {
            // No listener? Create a new one and bind it if necessary (otherwise, this will happen when
            // the AccessibilityService is started).
            ListenerConnection listener = new ListenerConnection(servicePackageName, serviceClassName);
            listener.enableApp(appToEnable);
            listeners.put(serviceClassName, listener);

            if (Misc.isAccessibilityServiceActive(context))
                context.getApplicationContext().bindService(listener.getListenerIntent(), listener, BIND_AUTO_CREATE);
        }
    }

    /**
     * Indicates whether a listener has an app enabled or not
     * @param context             App context
     * @param serviceClassName    Service class name to identify the listener
     * @param appToCheck          App to check enabled status
     * @return True if enabled, false otherwise
     */
    public static boolean appEnabledOnListener(Context context, String serviceClassName, String appToCheck) {
        if (!listeners.containsKey(serviceClassName))
            return false;

        ListenerConnection listener = listeners.get(serviceClassName);
        return listener.isAppEnabled(appToCheck);
    }

    /**
     * Disables listening for the provided app to be disabled. If other apps on the listener are still enabled,
     * the listener will keep running for the other apps. If not, the listener will be removed as well.
     * @param context             App context
     * @param serviceClassName    Full class name (including all packages) of the remote service
     * @param appToDisable        App to stop listening to
     */
    public static void removeListenerForApp(Context context, String serviceClassName, String appToDisable) {
        if (!listeners.containsKey(serviceClassName))
            return;

        ListenerConnection listener = listeners.get(serviceClassName);
        listener.disableApp(appToDisable);
        if (!listener.hasEnabledApps())
            listeners.remove(serviceClassName);
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

            // Changed app?
            checkPackageChanged();

            // Handle event detection
            AppDetectionData detectionData = multiLoader.get(packageName);
            if (detectionData != null)
                detectionData.performChecks(event, getRootInActiveWindow(), currentActivity);
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
     * Stores the name of the current activity in {@link CoastDoveService#currentActivity}
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
     * Check whether the package of the active app has changed. If so, send AppClosed
     * and AppOpened events for the according apps
     */
    private void checkPackageChanged() {
        int slashPos = currentActivity.indexOf('/');
        String activityPackageName = currentActivity.substring(0, slashPos < 0 ? 0 : slashPos);
        if (!activityPackageName.equals(previousPackageName)) {
            // Clean up previous app
            AppDetectionData previousDetectionData = multiLoader.get(previousPackageName);
            if (previousDetectionData != null) {
                screenStateReceiver.setCurrentDetectionData(null);
                previousDetectionData.onAppClosed();
            }

            // Init new app
            AppDetectionData currentDetectionData = multiLoader.get(activityPackageName);
            if (currentDetectionData != null) {
                screenStateReceiver.setCurrentDetectionData(currentDetectionData);
                currentDetectionData.onAppOpened();
            }
        }
        this.previousPackageName = activityPackageName;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        // Needed to receive screen off/on events
        this.screenStateReceiver = new ScreenStateReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(this.screenStateReceiver, filter);

        // Register all modules that have been enabled prior to activation of the
        // Accessibility Service
        for (ListenerConnection listener : listeners.values()) {
            Intent listenerIntent = listener.getListenerIntent();
            getApplicationContext().bindService(listenerIntent, listener, Context.BIND_AUTO_CREATE);
        }

        this.currentActivity = "";
        this.previousPackageName = "";

        service = this;
    }

    @Override
    public void onDestroy() {
        // Remove screen on/off listener
        unregisterReceiver(this.screenStateReceiver);
        // Unregister all modules
        for (ListenerConnection listener : listeners.values())
            getApplicationContext().unbindService(listener);

        service = null;
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {

    }
}
