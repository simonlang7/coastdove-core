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

package de.uni_bonn.detectappscreen.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import de.uni_bonn.detectappscreen.R;

/**
 * ListFragment displayed in the DetectableAppDetailsActivity,
 * shows a list of collected usage data for the according app
 */
public class AppUsageDataListFragment extends FileListFragment {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
    }

    @Override
    public Loader<ArrayList<String>> onCreateLoader(int id, Bundle args) {
        String subDirectory = ((DetectableAppDetailsActivity)getActivity()).getAppPackageName();
        return new FileListLoader(getActivity(), getString(R.string.external_folder_name),
                subDirectory + "/" + getString(R.string.app_usage_data_folder_name), ".json");
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<String>> loader) {
    }


    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        final String item = (String)listView.getItemAtPosition(position);

        String appPackageName = ((DetectableAppDetailsActivity)getActivity()).getAppPackageName();

        Intent intent = new Intent(getActivity(), AppUsageDataDetailsActivity.class);
        intent.putExtra(getString(R.string.extras_package_name), appPackageName);
        intent.putExtra(getString(R.string.extras_filename), item);
        startActivity(intent);
    }
    @Override
    protected void addProgressBarToViewGroup() {
        ViewGroup root = (ViewGroup)getActivity().findViewById(R.id.fragment_app_usage_data_file_list);
        root.addView(this.progressBar);
    }
}
