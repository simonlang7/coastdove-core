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

import android.content.ContentValues;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageContract;

/**
 * Data gathered when a TYPE_VIEW_CLICKED or TYPE_VIEW_SCROLLED event occurs
 */
public class InteractionEventData {
    private String androidID;
    private String text;
    private String className;

    public InteractionEventData(JSONObject dataJSON) {
        this.androidID = null;
        this.text = null;
        this.className = null;
        try {
            this.androidID = dataJSON.getString("androidID");
            this.text = dataJSON.getString("text");
            this.className = dataJSON.getString("className");
        } catch (JSONException e) {
            Log.e("InteractionEventData", "Unable to read from JSONObject: " + e.getMessage());
        }
    }

    public InteractionEventData(String androidID, String text, String className) {
        this.androidID = androidID;
        this.text = text;
        this.className = className;
    }

    public InteractionEventData(AccessibilityNodeInfo nodeInfo) {
        this.androidID = nodeInfo.getViewIdResourceName() != null ? nodeInfo.getViewIdResourceName() : "";
        this.text = nodeInfo.getText() != null ? nodeInfo.getText().toString().replaceAll("\n", " ") : "";
        this.className = nodeInfo.getClassName() != null ? nodeInfo.getClassName().toString() : "";
    }

    @Override
    public String toString() {
        String idString = androidID == null ? "" : "ID: " + androidID;
        String textSep = idString.equals("") ? "" : ", ";
        String textString = text == null ? "" : textSep + "Text: " + text;
        String classSep = (idString.equals("") || textString.equals("")) ? "" : ", ";
        String classString = className == null ? "" : classSep + "Class: " + className;

        return "(" + idString + textString + classString + ")";
    }

    public boolean equals(InteractionEventData other) {
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
            Log.e("InteractionEventData", "Unable to create JSONObject for " + androidID + " (" + text + "): " + e.getMessage());
        }

        return result;
    }

    public ContentValues toContentValues(long dataEntryID) {
        ContentValues values = new ContentValues();
        values.put(AppUsageContract.InteractionDetailsTable.COLUMN_NAME_ANDROID_ID, this.androidID);
        values.put(AppUsageContract.InteractionDetailsTable.COLUMN_NAME_TEXT, this.text);
        values.put(AppUsageContract.InteractionDetailsTable.COLUMN_NAME_CLASS_NAME, this.className);
        values.put(AppUsageContract.InteractionDetailsTable.COLUMN_NAME_DATA_ENTRY_ID, dataEntryID);
        return values;
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
