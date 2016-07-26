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
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data gathered when a TYPE_VIEW_CLICKED event occurs
 */
public class ClickedEventData {
    private String androidID;
    private String text;
    private String className;

    public ClickedEventData(JSONObject dataJSON) {
        this.androidID = null;
        this.text = null;
        this.className = null;
        try {
            this.androidID = dataJSON.getString("androidID");
            this.text = dataJSON.getString("text");
            this.className = dataJSON.getString("className");
        } catch (JSONException e) {
            Log.e("ClickedEventData", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    public ClickedEventData(String androidID, String text, String className) {
        this.androidID = androidID;
        this.text = text;
        this.className = className;
    }

    public ClickedEventData(AccessibilityNodeInfo nodeInfo) {
        this.androidID = nodeInfo.getViewIdResourceName() != null ? nodeInfo.getViewIdResourceName() : "";
        this.text = nodeInfo.getText() != null ? nodeInfo.getText().toString().replaceAll("\n", " ") : "";
        this.className = nodeInfo.getClassName() != null ? nodeInfo.getClassName().toString() : "";
    }

    @Override
    public String toString() {
        return "(ID: " + androidID + ", Text: " + text + ", Class: " + className + ")";
    }

    public boolean equals(ClickedEventData other) {
        return this.androidID.equals(other.androidID) &&
                this.text.equals(other.text) &&
                this.className.equals(other.className);
    }

    public JSONObject toJSON() {
        JSONObject result = new JSONObject();

        try {
            result.put("androidID", this.androidID);
            result.put("text", this.text);
            result.put("className", this.className);
        } catch (JSONException e) {
            Log.e("ClickedEventData", "Unable to create JSONObject for " + androidID + " (" + text + "): " + e.getMessage());
        }

        return result;
    }

    public String getClassName() {
        return className;
    }

    public String getText() {
        return text;
    }

    public String getAndroidID() {
        return androidID;
    }
}
