package de.uni_bonn.detectappscreen.app_usage;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Data entry containing a detected click at a certain point during app usage
 */
public class ClickDataEntry extends AppUsageDataEntry {

    /** Elements clicked */
    private Set<ClickedEventData> detectedClick;

    public ClickDataEntry(Date timestamp, String activity, Set<ClickedEventData> detectedClick) {
        super(timestamp, activity);
        this.detectedClick = detectedClick;
    }

    public ClickDataEntry(JSONObject entryJSON) {
        super(entryJSON);

        this.detectedClick = new CopyOnWriteArraySet<>();
        try {
            JSONArray detectedClickJSON = entryJSON.getJSONArray("detectedClick");
            for (int i = 0; i < detectedClickJSON.length(); ++i) {
                JSONObject clickDataJSON = detectedClickJSON.getJSONObject(i);
                ClickedEventData clickData = new ClickedEventData(clickDataJSON);
                this.detectedClick.add(clickData);
            }
        } catch (JSONException e) {
            Log.e("ClickDataEntry", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    @Override
    public boolean equals(AppUsageDataEntry other) {
        if (!super.equals(other))
            return false;

        if (!(other instanceof ClickDataEntry))
            return false;

        ClickDataEntry otherEntry = (ClickDataEntry)other;
        if (this.detectedClick.size() != otherEntry.detectedClick.size())
            return false;

        outer: for (ClickedEventData clickData : this.detectedClick) {
            for (ClickedEventData otherClickData : otherEntry.detectedClick) {
                if (clickData.equals(otherClickData))
                    continue outer;
            }
            return false;
        }

        return true;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject result = super.toJSON();
        try {
            JSONArray detectedClickJSON = new JSONArray();
            for (ClickedEventData clickData : detectedClick)
                detectedClickJSON.put(clickData.toJSON());
            result.put("detectedClick", detectedClickJSON);
        } catch (JSONException e) {
            Log.e("ClickDataEntry", "Unable to create JSONObject: " + e.getMessage());
        }
        return result;
    }

    @Override
    public String getType() {
        return "Click";
    }

    @Override
    public String getContent() {
        return detectedClick.toString();
    }

    public Set<ClickedEventData> getDetectedClick() {
        return this.detectedClick;
    }
}
