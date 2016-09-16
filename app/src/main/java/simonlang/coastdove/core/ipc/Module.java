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

package simonlang.coastdove.core.ipc;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Contains data for a Coast Dove module
 */
public class Module {
    public final String moduleName;
    public final String servicePackageName;
    public final String serviceClassName;
    public final ArrayList<String> associatedApps;

    public Module(String moduleName, String serviceClassName, Collection<String> associatedApps, String servicePackageName) {
        this.moduleName = moduleName;
        this.servicePackageName = servicePackageName;
        this.serviceClassName = serviceClassName;
        this.associatedApps = new ArrayList<>(associatedApps);
    }

    @Override
    public String toString() {
        return moduleName + " (associated with: " + associatedApps + ")";
    }
}
