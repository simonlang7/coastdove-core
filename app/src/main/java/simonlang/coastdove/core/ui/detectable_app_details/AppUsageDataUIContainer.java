/*  Coast Dove
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

package simonlang.coastdove.core.ui.detectable_app_details;

/**
 * Contains information on an app usage data object, needed for the UI
 */
public class AppUsageDataUIContainer {
    /** Primary key of the AppUsageData (App) in the SQLite database */
    public int id;
    /** Timestamp of the app usage data */
    public String timestamp;
    /** Duration of the app usage data */
    public String duration;

    public AppUsageDataUIContainer() {
    }

    public AppUsageDataUIContainer(int id, String timestamp, String duration) {
        this.id = id;
        this.timestamp = timestamp;
        this.duration = duration;
    }
}
