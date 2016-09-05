package de.uni_bonn.detectappscreen.ui.detectable_app_details;

/**
 * Contains information on an app usage data object, needed for the UI
 */
public class AppUsageDataUIContainer {
    /** Primary key of the AppUsageData (App) in the SQLite database */
    public int id;
    /** Timestamp of the app usage data */
    public String timestamp;
    /** Duration of the app usage data */
    public String duration;

    public AppUsageDataUIContainer() {
    }

    public AppUsageDataUIContainer(int id, String timestamp, String duration) {
        this.id = id;
        this.timestamp = timestamp;
        this.duration = duration;
    }
}
