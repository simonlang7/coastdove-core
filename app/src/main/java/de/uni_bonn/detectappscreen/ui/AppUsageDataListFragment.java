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
