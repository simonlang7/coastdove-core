package de.uni_bonn.detectappscreen;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

/**
 * ListFragment that displays a progress bar while loading its contents
 */
public class DataEntryListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<AppUsageData> {

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

        getListView().setEmptyView(this.progressBar);
        addProgressBarToViewGroup();

        AppUsageDataDetailsActivity activity = (AppUsageDataDetailsActivity)getActivity();
        this.appPackageName = activity.getAppPackageName();
        this.filename = activity.getFilename();

        this.adapter = new DataEntryListAdapter(getActivity(), this.appPackageName);
        setListAdapter(this.adapter);


        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_list, container, false);
    }

    @Override
    public Loader<AppUsageData> onCreateLoader(int id, Bundle args) {
        return new AppUsageDataLoader(getActivity(), this.appPackageName, this.filename);
    }

    @Override
    public void onLoadFinished(Loader<AppUsageData> loader, AppUsageData data) {
        this.adapter.clear();
        this.adapter.addAll(data.getDataEntries());
        this.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<AppUsageData> loader) {
    }

    protected void addProgressBarToViewGroup() {
        ViewGroup viewGroup = (ViewGroup)getActivity().findViewById(R.id.fragment_data_entries);
        viewGroup.addView(this.progressBar);
    }
}
