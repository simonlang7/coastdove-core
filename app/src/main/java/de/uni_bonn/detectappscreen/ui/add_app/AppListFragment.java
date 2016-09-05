package de.uni_bonn.detectappscreen.ui.add_app;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.detection.AppDetectionData;
import de.uni_bonn.detectappscreen.detection.AppDetectionDataLoader;
import de.uni_bonn.detectappscreen.detection.DetectAppScreenAccessibilityService;
import de.uni_bonn.detectappscreen.ui.LoadableListFragment;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.ui.detectable_app_details.DetectableAppDetailsActivity;
import de.uni_bonn.detectappscreen.utility.FileHelper;
import de.uni_bonn.detectappscreen.utility.MultipleObjectLoader;

/**
 * ListFragment displayed in AddAppActivity, displays apps installed on the device
 */
public class AppListFragment extends LoadableListFragment<ApplicationInfo> {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MultipleObjectLoader<AppDetectionData> multiLoader = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader();
        multiLoader.updateLoadingInfoUIElements(AddAppActivity.ORIGIN,
                getActivity(), this.adapter);
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
        MultipleObjectLoader<AppDetectionData> multiLoader = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader();

        if (!cacheExists && multiLoader.getLoadingInfo(item.packageName) == null) {
            // Create app detection data
            LoadingInfo loadingInfo = new LoadingInfo(getActivity().getApplicationContext(),
                    item.publicSourceDir.hashCode(), AddAppActivity.ORIGIN);
            loadingInfo.setUIElements(getActivity(), adapter);

            AppDetectionDataLoader loader = new AppDetectionDataLoader(item.packageName, multiLoader, item.publicSourceDir, getActivity(), loadingInfo);

            multiLoader.startLoading(item.packageName, loader, loadingInfo);
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
