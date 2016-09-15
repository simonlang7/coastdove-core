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

package simonlang.coastdove.core.detection;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Class to extract data from a node info
 */
public abstract class NodeInfoDataExtractor<T> {
    /**
     * Extracts data from a given node info.
     * @param nodeInfo    Node info to extract data from
     * @return Data extracted
     */
    public abstract T extractData(AccessibilityNodeInfo nodeInfo);
}