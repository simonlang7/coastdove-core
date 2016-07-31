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

package de.uni_bonn.detectappscreen.analyze;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import de.uni_bonn.detectappscreen.app_usage.ActivityData;
import de.uni_bonn.detectappscreen.app_usage.AppUsageDataEntry;

/**
 * Contains meta information regarding a detectable app, such as entry activities
 */
public class AppMetaInformation {
    /** Package name of the app */
    private String appPackageName;
    /** Activities that are entry points to the app from a launcher */
    private List<String> mainActivities;

    /**
     * Creates AppMetaInformation with the given data
     */
    public AppMetaInformation(String appPackageName, List<String> mainActivities) {
        this.appPackageName = appPackageName;
        this.mainActivities = mainActivities;
    }

    /**
     * Creates AppMetaInformation from a JSONObject
     */
    public AppMetaInformation(JSONObject appMetaInformationJSON) {
        this.mainActivities = new LinkedList<>();
        try {
            JSONArray mainActivitiesJSON = appMetaInformationJSON.getJSONArray("mainActivities");
            for (int i = 0; i < mainActivitiesJSON.length(); ++i)
                this.mainActivities.add(mainActivitiesJSON.getString(i));
        } catch (JSONException e) {
            Log.e("AppMetaInformation", "Error reading from JSONObject: " + e.getMessage());
        }
    }

    public boolean isMainActivity(String activity) {
        for (String mainActivity : this.mainActivities) {
            if (mainActivity.replaceAll("/", "").contains(activity))
                return true;
        }
        return false;
    }

    public boolean isMainActivity(ActivityData data) {
        return isMainActivity(data.getShortenedActivity());
    }

    /** Package name of the app */
    public String getAppPackageName() {
        return appPackageName;
    }

    /** Activities that are entry points to the app from a launcher */
    public List<String> getMainActivities() {
        return mainActivities;
    }
}
