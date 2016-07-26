/*  DetectAppScreen
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
*/

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
