package de.uni_bonn.detectappscreen.app_usage;

import org.json.JSONObject;

import java.util.Date;

/**
 * Data entry containing activity information
 */
public class ActivityDataEntry extends AppUsageDataEntry {

    public ActivityDataEntry(Date timestamp, String activity) {
        super(timestamp, activity);
    }

    public ActivityDataEntry(JSONObject entryJSON) {
        super(entryJSON);
    }

    @Override
    public String getType() {
        return "Activity";
    }

    @Override
    public String getContent() {
        return "";//getActivity();
    }
}
