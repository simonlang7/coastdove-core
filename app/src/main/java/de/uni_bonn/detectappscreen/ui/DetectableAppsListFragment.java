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
