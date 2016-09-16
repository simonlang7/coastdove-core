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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.view.ViewGroup;

import java.util.ArrayList;

import simonlang.coastdove.core.R;
import simonlang.coastdove.core.ipc.Module;
import simonlang.coastdove.core.ui.LoadableListFragment;

/**
 * ListFragment displayed in DetectableAppDetailsActivity, shows a list
 * of modules associated with the app
 */
public class ModuleListFragment extends LoadableListFragment<Module> {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.loaderID = 300;
    }

    @Override
    public Loader<ArrayList<Module>> onCreateLoader(int id, Bundle args) {
        String appPackageName = ((DetectableAppDetailsActivity)getActivity()).getAppPackageName();
        return new SQLiteTableLoader(getActivity(), appPackageName);
    }

    @Override
    protected void setUpListAdapter() {
        String appPackageName = ((DetectableAppDetailsActivity)getActivity()).getAppPackageName();
        this.adapter = new ModuleListAdapter(getActivity(), appPackageName);
        setListAdapter(this.adapter);
    }

    @Override
    protected void addProgressBarToViewGroup() {
        ViewGroup root = (ViewGroup)getActivity().findViewById(R.id.fragment_module_list);
        root.addView(this.progressBar);
    }
}
