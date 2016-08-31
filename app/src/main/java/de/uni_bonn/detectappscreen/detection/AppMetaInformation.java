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

package de.uni_bonn.detectappscreen.detection;


import java.io.Serializable;
import java.util.Collection;

import de.uni_bonn.detectappscreen.app_usage.ActivityData;

/**
 * Contains meta information regarding a detectable app, such as entry activities
 */
public class AppMetaInformation implements Serializable {
    private static final long serialVersionUID = 974164654965624505L;

    /** Package name of the app */
    private String appPackageName;
    /** Activities that are entry points to the app from a launcher */
    private Collection<String> mainActivities;

    /**
     * Creates AppMetaInformation with the given data
     */
    public AppMetaInformation(String appPackageName, Collection<String> mainActivities) {
        this.appPackageName = appPackageName;
        this.mainActivities = mainActivities;
    }

    /**
     * Tells whether the given activity is a possible entry point from a launcher
     * @param activity    Activity to check
     * @return True if the activity is a main activity
     */
    public boolean isMainActivity(String activity) {
        for (String mainActivity : this.mainActivities) {
            if (mainActivity.replaceAll("/", "").contains(activity))
                return true;
        }
        return false;
    }

    /**
     * Tells whether the given activity is a possible entry point from a launcher
     * @param data    Activity to check
     * @return True if the activity is a main activity
     */
    public boolean isMainActivity(ActivityData data) {
        return isMainActivity(data.getShortenedActivity());
    }

    /** Package name of the app */
    public String getAppPackageName() {
        return appPackageName;
    }

    /** Activities that are entry points to the app from a launcher */
    public Collection<String> getMainActivities() {
        return mainActivities;
    }
}
