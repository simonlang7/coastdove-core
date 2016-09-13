package simonlang.coastdove.overlays;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

import simonlang.coastdove.detection.InteractionEventData;
import simonlang.coastdove.utility.CollatorWrapper;

/**
 * Listener to be registered in CoastDoveNotifier
 */
public abstract class CoastDoveListener {
    /** Last activity detected, or "" if none so far */
    private transient volatile String lastActivity;
    /** Last layouts detected (empty set if none) */
    private transient volatile Set<String> lastLayouts;
    /** Last interaction detected (empty set if none) */
    private transient volatile Set<InteractionEventData> lastInteraction;
    /** Last notification detected, or "" if none so far */
    private transient volatile String lastNotification;
    /** Whether the screen is currently off, according to the last
     *  screen state detected (false by default) */
    private transient volatile boolean screenOff;

    /**
     * Initializes all members to empty or default values (empty sets, "", false)
     * to make sure they don't need to be checked for null
     */
    public CoastDoveListener() {
        this.lastActivity = "";
        this.lastLayouts = new TreeSet<>(new CollatorWrapper());
        this.lastInteraction = new CopyOnWriteArraySet<>();
        this.lastNotification = "";
        this.screenOff = false;
    }

    void activityDetected(String activity) {
        this.lastActivity = activity;
        onActivityDetected(lastActivity);
    }
    void layoutsDetected(Set<String> layouts) {
        this.lastLayouts = new TreeSet<>(new CollatorWrapper());
        this.lastLayouts.addAll(layouts);
        onLayoutsDetected(lastLayouts);
    }
    void interactionDetected(Set<InteractionEventData> interaction) {
        this.lastInteraction = new CopyOnWriteArraySet<>(interaction);
        onInteractionDetected(lastInteraction);
    }
    void notificationDetected(String notification) {
        this.lastNotification = notification;
        onNotificationDetected(notification);
    }
    void screenStateDetected(boolean screenOff) {
        this.screenOff = screenOff;
        onScreenStateDetected(screenOff);
    }

    /**
     * Called by CoastDoveNotifier whenever a new activity has been detected
     * @param activity    The activity detected
     */
    protected void onActivityDetected(String activity) { }

    /**
     * Called by CoastDoveNotifier whenever a new set of layouts has been detected
     * @param layouts    Layouts detected
     */
    protected void onLayoutsDetected(Set<String> layouts) { }

    /**
     * Called by CoastDoveNotifier whenever a new interaction has been detected
     * @param interaction    Interaction detected
     */
    protected void onInteractionDetected(Set<InteractionEventData> interaction) { }

    /**
     * Called by CoastDoveNotifier whenever a new notification has been detected
     * @param notification    Notification detected
     */
    protected void onNotificationDetected(String notification) { }

    /**
     * Called by CoastDoveNotifier whenever the screen state has changed (turned off or on)
     * @param screenOff    Whether the screen has been turned off or on (true if off)
     */
    protected void onScreenStateDetected(boolean screenOff) { }

    /** Last activity detected, or "" if none so far */
    public final String getLastActivity() {
        return lastActivity;
    }

    /** Last layouts detected (empty set if none) */
    public final Set<String> getLastLayouts() {
        return lastLayouts;
    }

    /** Last interaction detected (empty set if none) */
    public final Set<InteractionEventData> getLastInteraction() {
        return lastInteraction;
    }

    /** Last notification detected, or "" if none so far */
    public final String getLastNotification() {
        return lastNotification;
    }

    /** Whether the screen is currently off, according to the last
     *  screen state detected (false by default) */
    public final boolean isScreenOff() {
        return screenOff;
    }
}
