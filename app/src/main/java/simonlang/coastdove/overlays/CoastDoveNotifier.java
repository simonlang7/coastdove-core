package simonlang.coastdove.overlays;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import simonlang.coastdove.detection.InteractionEventData;

/**
 * Class for testing overlays
 */
public class CoastDoveNotifier {
    private static final Map<String, Collection<CoastDoveListener>> listenerMap =
            new ConcurrentHashMap<>();

    public static void registerListener(String appPackageName, CoastDoveListener listener) {
        synchronized (listenerMap) {
            Collection<CoastDoveListener> listeners = listenerMap.get(appPackageName);
            if (listeners == null) {
                listeners = new LinkedList<>();
                listenerMap.put(appPackageName, listeners);
            }
            listeners.add(listener);
        }
    }

    public static void removeAllListeners(String appPackageName) {
        synchronized (listenerMap) {
            listenerMap.remove(appPackageName);
        }
    }

    public static void removeListener(String appPackageName, CoastDoveListener listener) {
        synchronized (listenerMap) {
            Collection<CoastDoveListener> listeners = listenerMap.get(appPackageName);
            if (listeners != null)
                listeners.remove(listener);
        }
    }

    public static void notifyAboutActivity(String appPackageName, String activity) {
        synchronized (listenerMap) {
            Collection<CoastDoveListener> listeners = listenerMap.get(appPackageName);
            if (listeners != null) {
                for (CoastDoveListener listener : listeners)
                    listener.activityDetected(activity);
            }
        }
    }

    public static void notifyAboutLayouts(String appPackageName, Set<String> layouts) {
        synchronized (listenerMap) {
            Collection<CoastDoveListener> listeners = listenerMap.get(appPackageName);
            if (listeners != null) {
                for (CoastDoveListener listener : listeners)
                    listener.layoutsDetected(layouts);
            }
        }
    }

    public static void notifyAboutInteraction(String appPackageName, Set<InteractionEventData> interaction) {
        synchronized (listenerMap) {
            Collection<CoastDoveListener> listeners = listenerMap.get(appPackageName);
            if (listeners != null) {
                for (CoastDoveListener listener : listeners)
                    listener.interactionDetected(interaction);
            }
        }
    }

    public static void notifyAboutNotification(String appPackageName, String notification) {
        synchronized (listenerMap) {
            Collection<CoastDoveListener> listeners = listenerMap.get(appPackageName);
            if (listeners != null) {
                for (CoastDoveListener listener : listeners)
                    listener.notificationDetected(notification);
            }
        }
    }

    public static void notifyAboutScreenState(String appPackageName, boolean screenOff) {
        synchronized (listenerMap) {
            Collection<CoastDoveListener> listeners = listenerMap.get(appPackageName);
            if (listeners != null) {
                for (CoastDoveListener listener : listeners)
                    listener.screenStateDetected(screenOff);
            }
        }
    }
}
