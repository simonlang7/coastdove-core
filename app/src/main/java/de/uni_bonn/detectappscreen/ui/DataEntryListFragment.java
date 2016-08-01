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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import de.uni_bonn.detectappscreen.analyze.AppUsageDataProcessor;
import de.uni_bonn.detectappscreen.app_usage.AppUsageDataProcessorLoader;
import de.uni_bonn.detectappscreen.R;

/**
 * ListFragment that displays a progress bar while loading its contents
 */
public class DataEntryListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<AppUsageDataProcessor> {

    private DataEntryListAdapter adapter;
    private ProgressBar progressBar;

    private String appPackageName;
    private String filename;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Progress bar while the list loads
        this.progressBar = new ProgressBar(getActivity());
        this.progressBar.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.weight = 2;
        this.progressBar.setLayoutParams(layoutParams);
        this.progressBar.setIndeterminate(true);

        ExpandableListView elv = (ExpandableListView)getView().findViewById(android.R.id.list);
        elv.setEmptyView(this.progressBar);
        addProgressBarToViewGroup();

        AppUsageDataDetailsActivity activity = (AppUsageDataDetailsActivity)getActivity();
        this.appPackageName = activity.getAppPackageName();
        this.filename = activity.getFilename();

        this.adapter = new DataEntryListAdapter(getActivity(), this.appPackageName);
        elv.setAdapter(this.adapter);

        elv.setGroupIndicator(null);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_entry_list, container, false);
    }

    @Override
    public Loader<AppUsageDataProcessor> onCreateLoader(int id, Bundle args) {
        return new AppUsageDataProcessorLoader(getActivity(), this.appPackageName, this.filename);
    }

    @Override
    public void onLoadFinished(Loader<AppUsageDataProcessor> loader, AppUsageDataProcessor data) {
        this.adapter.clear();
        this.adapter.addAll(data.getAppUsageMetaData());
        this.progressBar.setVisibility(View.GONE);
        AppUsageDataDetailsActivity activity = (AppUsageDataDetailsActivity)getActivity();
        activity.setAppUsageDataProcessor(data);
    }

    @Override
    public void onLoaderReset(Loader<AppUsageDataProcessor> loader) {
    }

    protected void addProgressBarToViewGroup() {
        ViewGroup viewGroup = (ViewGroup)getActivity().findViewById(R.id.fragment_data_entries);
        viewGroup.addView(this.progressBar);
    }
}
