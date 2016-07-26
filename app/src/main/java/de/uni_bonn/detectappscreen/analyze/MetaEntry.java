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

import java.util.LinkedList;
import java.util.List;

import de.uni_bonn.detectappscreen.app_usage.AppUsageDataEntry;

/**
 * Meta data for app usage data entries, containing additional information needed
 * for formatting, todo: among other things
 */
public class MetaEntry {
    private List<AppUsageDataEntry> dataEntries;
    private int level;
    public MetaEntry() {
        this.dataEntries = new LinkedList<>();
        this.level = 0;
    }
    public MetaEntry(List<AppUsageDataEntry> entries) {
        this.dataEntries = entries;
        this.level = 0;
    }
    public MetaEntry(List<AppUsageDataEntry> entries, int level) {
        this.dataEntries = entries;
        this.level = level;
    }

    public List<AppUsageDataEntry> getDataEntries() {
        return dataEntries;
    }

    public int getLevel() {
        return level;
    }
}