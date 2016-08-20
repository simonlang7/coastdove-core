package de.uni_bonn.detectappscreen.ui.add_app;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.setup.LayoutCollection;
import de.uni_bonn.detectappscreen.setup.LayoutCollectionLoader;
import de.uni_bonn.detectappscreen.ui.LoadableListFragment;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.utility.MultipleObjectLoader;

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

        ProgressBar progressBar = (ProgressBar)getActivity().findViewById(R.id.progressBar);
        LoadingInfo loadingInfo = new LoadingInfo(getActivity(), item.publicSourceDir.hashCode(),
                progressBar, true);

        MultipleObjectLoader<LayoutCollection> multiLoader = LayoutCollection.getLayoutCollectionMultipleObjectLoader();
        LayoutCollectionLoader loader = new LayoutCollectionLoader(getActivity(), item.packageName, item.publicSourceDir,
                multiLoader, 1.0f, loadingInfo);
        multiLoader.startLoading(item.packageName, loader);
    }

    @Override
    protected void addProgressBarToViewGroup() {
        ViewGroup root = (ViewGroup)getActivity().findViewById(R.id.fragment_app_list);
        root.addView(this.progressBar);
    }
}
