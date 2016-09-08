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

package simonlang.coastdove.ui.add_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import simonlang.coastdove.R;
import simonlang.coastdove.detection.AppDetectionData;
import simonlang.coastdove.detection.AppDetectionDataLoader;
import simonlang.coastdove.detection.CoastAccessibilityService;
import simonlang.coastdove.ui.LoadableListFragment;
import simonlang.coastdove.ui.LoadingInfo;
import simonlang.coastdove.ui.detectable_app_details.DetectableAppDetailsActivity;
import simonlang.coastdove.utility.FileHelper;
import simonlang.coastdove.utility.Misc;
import simonlang.coastdove.utility.MultipleObjectLoader;

/**
 * ListFragment displayed in AddAppActivity, displays apps installed on the device
 */
public class AppListFragment extends LoadableListFragment<ApplicationInfo> {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MultipleObjectLoader<AppDetectionData> multiLoader = CoastAccessibilityService.getAppDetectionDataMultiLoader();
        multiLoader.updateLoadingInfoUIElements(AddAppActivity.ORIGIN,
                getActivity(), this.adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        int scrollPosition = preferences.getInt(Misc.ADD_APP_SCROLL_POSITION_PREF, 0);

        getListView().smoothScrollToPosition(scrollPosition);
    }

    @Override
    public void onPause() {
        super.onPause();

        int scrollPosition = getListView().getFirstVisiblePosition();
        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        preferences.edit().putInt(Misc.ADD_APP_SCROLL_POSITION_PREF, scrollPosition).apply();
    }

    @Override
    public Loader<ArrayList<ApplicationInfo>> onCreateLoader(int id, Bundle args) {
        return new AppListLoader(getActivity());
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<ApplicationInfo>> loader) {
    }

    @Override
    protected void setUpListAdapter() {
        this.adapter = new AppListAdapter(getActivity(), R.layout.list_item_app);
        setListAdapter(this.adapter);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        final ApplicationInfo item = (ApplicationInfo)listView.getItemAtPosition(position);
        boolean cacheExists = FileHelper.appDetectionDataExists(getActivity(), item.packageName);
        MultipleObjectLoader<AppDetectionData> multiLoader = CoastAccessibilityService.getAppDetectionDataMultiLoader();

        if (!cacheExists && multiLoader.getLoadingInfo(item.packageName) == null) {
            if (multiLoader.getNumberLoading() == 0) {
                // TODO: put in queue, load one after another
                // Create app detection data
                LoadingInfo loadingInfo = new LoadingInfo(getActivity().getApplicationContext(),
                        item.publicSourceDir.hashCode(), AddAppActivity.ORIGIN);
                loadingInfo.setUIElements(getActivity(), adapter);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                boolean replacePrivateData = Misc.getPreferenceBoolean(prefs, item.packageName,
                        getActivity().getString(R.string.pref_replace_private_data), Misc.DEFAULT_REPLACE_PRIVATE_DATA);
                AppDetectionDataLoader loader = new AppDetectionDataLoader(item.packageName, multiLoader, item.publicSourceDir, replacePrivateData, getActivity(), loadingInfo);

                multiLoader.startLoading(item.packageName, loader, loadingInfo);
            }
        }
        else {
            // Open details if data exist
            Intent intent = new Intent(getActivity(), DetectableAppDetailsActivity.class);
            intent.putExtra(getString(R.string.extras_package_name), item.packageName);
            startActivity(intent);
        }
    }

    @Override
    protected void addProgressBarToViewGroup() {
        ViewGroup root = (ViewGroup)getActivity().findViewById(R.id.fragment_app_list);
        root.addView(this.progressBar);
    }
}
