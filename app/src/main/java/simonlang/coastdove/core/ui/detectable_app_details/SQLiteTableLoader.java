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

package simonlang.coastdove.core.ui.detectable_app_details;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.List;

import simonlang.coastdove.core.ipc.Module;
import simonlang.coastdove.core.ipc.ModuleRegisteringService;

/**
 * Loads a list of entries from an SQLite table
 */
public class SQLiteTableLoader extends AsyncTaskLoader<ArrayList<Module>> {
    private String appPackageName;

    public SQLiteTableLoader(Context context, String appPackageName) {
        super(context);
        this.appPackageName = appPackageName;
    }

    @Override
    public ArrayList<Module> loadInBackground() {
        List<Module> modules = ModuleRegisteringService.queryModules(getContext());
        ArrayList<Module> data = new ArrayList<>();
        for (Module module : modules) {
            if (module.associatedApps.contains(appPackageName) || module.associatedApps.contains("*"))
                data.add(module);
        }
        return data;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }
}
