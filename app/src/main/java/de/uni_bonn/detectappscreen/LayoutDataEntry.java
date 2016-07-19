package de.uni_bonn.detectappscreen;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

/**
 * Data entry containing detected layouts at a certain point during app usage
 */
public class LayoutDataEntry extends AppUsageDataEntry {

    /** Layouts detected */
    private Set<String> detectedLayouts;

    public LayoutDataEntry(Date timestamp, String activity, Set<String> detectedLayouts) {
        super(timestamp, activity);
        this.detectedLayouts = detectedLayouts;
    }

    public LayoutDataEntry(JSONObject entryJSON) {
        super(entryJSON);

        this.detectedLayouts = new TreeSet<>(new CollatorWrapper());
        try {
            JSONArray detectedLayoutsJSON = entryJSON.getJSONArray("detectedLayouts");
            for (int i = 0; i < detectedLayoutsJSON.length(); ++i) {
                String layout = detectedLayoutsJSON.getString(i);
                this.detectedLayouts.add(layout);
            }
        } catch (JSONException e) {
            Log.e("LayoutDataEntry", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    @Override
    public boolean equals(AppUsageDataEntry other) {
        if (!super.equals(other))
            return false;

        if (!(other instanceof LayoutDataEntry))
            return false;

        LayoutDataEntry otherEntry = (LayoutDataEntry)other;
        if (this.detectedLayouts.size() != otherEntry.detectedLayouts.size())
            return false;

        outer: for (String layout : this.detectedLayouts) {
            for (String otherLayout : otherEntry.detectedLayouts) {
                if (layout.equals(otherLayout))
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
            result.put("detectedLayouts", new JSONArray(this.detectedLayouts));
        } catch (JSONException e) {
            Log.e("LayoutDataEntry", "Unable to create JSONObject: " + e.getMessage());
        }
        return result;
    }

    @Override
    public String getType() {
        return "Layouts";
    }

    @Override
    public String getContent() {
        return detectedLayouts.toString();
    }

    /** Layouts detected */
    public Set<String> getDetectedLayouts() {
        return detectedLayouts;
    }
}
