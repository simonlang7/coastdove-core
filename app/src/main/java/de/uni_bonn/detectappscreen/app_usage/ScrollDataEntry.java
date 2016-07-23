package de.uni_bonn.detectappscreen.app_usage;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Data entry containing a scroll event at a certain point during app usage
 */
public class ScrollDataEntry extends AppUsageDataEntry {

    /** Element scrolled */
    private String scrolledElement;

    public ScrollDataEntry(Date timestamp, String activity, String scrolledElement) {
        super(timestamp, activity);
        this.scrolledElement = scrolledElement;
    }

    public ScrollDataEntry(JSONObject entryJSON) {
        super(entryJSON);
        try {
            this.scrolledElement = entryJSON.getString("scrolledElement");
        } catch (JSONException e) {
            Log.e("ScrollDataEntry", "Unable to read from JSONObject: " + e.getMessage());
        }
    }


    @Override
    public boolean equals(AppUsageDataEntry other) {
        if (!super.equals(other))
            return false;

        if (!(other instanceof ScrollDataEntry))
            return false;

        ScrollDataEntry otherEntry = (ScrollDataEntry)other;
        if (!this.scrolledElement.equals(otherEntry.scrolledElement))
            return false;

        return true;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject result = super.toJSON();
        try {
            result.put("scrolledElement", this.scrolledElement);
        } catch (JSONException e) {
            Log.e("ScrollDataEntry", "Unable to create JSONObject: " + e.getMessage());
        }
        return result;
    }

    @Override
    public String getType() {
        return "Scrolling";
    }

    @Override
    public String getContent() {
        return this.scrolledElement;
    }
}
