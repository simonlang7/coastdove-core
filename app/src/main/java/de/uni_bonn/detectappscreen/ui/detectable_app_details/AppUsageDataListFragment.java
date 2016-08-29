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

package de.uni_bonn.detectappscreen.ui.detectable_app_details;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.app_usage.sql.AppUsageDbHelper;
import de.uni_bonn.detectappscreen.app_usage.sql.SQLiteDataRemover;
import de.uni_bonn.detectappscreen.ui.SQLiteTableLoader;
import de.uni_bonn.detectappscreen.ui.app_usage_data_details.AppUsageDataDetailsActivity;
import de.uni_bonn.detectappscreen.ui.LoadableListFragment;
import de.uni_bonn.detectappscreen.ui.FileListLoader;

/**
 * ListFragment displayed in the DetectableAppDetailsActivity,
 * shows a list of collected usage data for the according app
 */
public class AppUsageDataListFragment extends LoadableListFragment<Pair<Integer, String>> {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.loaderID = 100;

        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                listAdapter().toggleSelected(position);
                mode.setTitle(listAdapter().selectedCount()+"");
                listAdapter().notifyDataSetChanged();
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.app_usage_data_context_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.item_context_delete:
                        List<Pair<Integer, String>> selectedItems = listAdapter().getSelectedItems();
                        List<Integer> selectedItemsPrimaryKeys = new LinkedList<>();
                        for (Pair<Integer, String> selectedItem : selectedItems)
                            selectedItemsPrimaryKeys.add(selectedItem.first);
                        new SQLiteDataRemover(getActivity(), selectedItemsPrimaryKeys).run();
                        Toast toast = Toast.makeText(getActivity(), getString(R.string.toast_data_removed), Toast.LENGTH_SHORT);
                        toast.show();
                        mode.finish();
                        getLoaderManager().restartLoader(loaderID, getArguments(), AppUsageDataListFragment.this);
                        getListView().invalidate();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                listAdapter().resetSelected();
            }
        });
    }

    @Override
    public Loader<ArrayList<Pair<Integer, String>>> onCreateLoader(int id, Bundle args) {
        String appPackageName = ((DetectableAppDetailsActivity)getActivity()).getAppPackageName();
        return new SQLiteTableLoader(getActivity(), appPackageName);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Pair<Integer, String>>> loader) {
    }


    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        final Pair<Integer, String> item = (Pair<Integer, String>)listView.getItemAtPosition(position);

        String appPackageName = ((DetectableAppDetailsActivity)getActivity()).getAppPackageName();

        Intent intent = new Intent(getActivity(), AppUsageDataDetailsActivity.class);
        intent.putExtra(getString(R.string.extras_package_name), appPackageName);
        intent.putExtra(getString(R.string.extras_timestamp), item.second);
        intent.putExtra(getString(R.string.extras_app_id), item.first);

        startActivity(intent);
    }

    private AppUsageDataListAdapter listAdapter() {
        return (AppUsageDataListAdapter)this.adapter;
    }

    @Override
    protected void setUpListAdapter() {
        this.adapter = new AppUsageDataListAdapter(getActivity(), android.R.layout.simple_list_item_1);
        setListAdapter(this.adapter);
    }

    @Override
    protected void addProgressBarToViewGroup() {
        ViewGroup root = (ViewGroup)getActivity().findViewById(R.id.fragment_app_usage_data_file_list);
        root.addView(this.progressBar);
    }
}
