package de.uni_bonn.detectappscreen.ui.add_app;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Comparator;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.ui.LoadableListFragment;

/**
 * ListFragment displayed in AddAppActivity, displays apps installed on the device
 */
public class AppListFragment extends LoadableListFragment<ApplicationInfo> {

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

        // todo: start loading apk here
    }

    @Override
    protected void addProgressBarToViewGroup() {
        ViewGroup root = (ViewGroup)getActivity().findViewById(R.id.fragment_app_list);
        root.addView(this.progressBar);
    }
}
