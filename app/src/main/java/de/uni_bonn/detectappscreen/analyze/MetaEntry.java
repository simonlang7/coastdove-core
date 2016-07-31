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

import android.support.annotation.NonNull;

import de.uni_bonn.detectappscreen.app_usage.ActivityData;

/**
 * Meta data for activity data objects, containing additional information needed
 * for formatting, todo: among other things
 */
public class MetaEntry {
    private ActivityData activityData;
    private int level;

    public MetaEntry(@NonNull ActivityData activityData) {
        this.activityData = activityData;
        this.level = 0;
    }
    public MetaEntry(@NonNull ActivityData activityData, int level) {
        this.activityData = activityData;
        this.level = level;
    }

    public ActivityData getActivityData() {
        return this.activityData;
    }

    public int getLevel() {
        return level;
    }
}