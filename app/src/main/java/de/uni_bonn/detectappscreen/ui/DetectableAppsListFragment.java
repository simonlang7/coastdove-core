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
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import de.uni_bonn.detectappscreen.R;

/**
 * ListFragment containing all available apps that can be detected by the accessibility service
 */
public class DetectableAppsListFragment extends FileListFragment {

    @Override
    public Loader<ArrayList<String>> onCreateLoader(int id, Bundle args) {
        return new FileListLoader(getActivity(), getString(R.string.external_folder_name), null, null);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<String>> loader) {
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        super.onListItemClick(listView, view, position, id);
        final String item = (String)listView.getItemAtPosition(position);

        Intent intent = new Intent(getActivity(), DetectableAppDetailsActivity.class);
        intent.putExtra(getString(R.string.extras_package_name), item);
        startActivity(intent);
    }

    @Override
    protected void addProgressBarToViewGroup() {
        ViewGroup root = (ViewGroup)getActivity().findViewById(R.id.fragment_detectable_apps_file_list);
        root.addView(this.progressBar);
    }

}
