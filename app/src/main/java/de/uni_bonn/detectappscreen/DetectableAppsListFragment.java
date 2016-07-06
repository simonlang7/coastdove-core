package de.uni_bonn.detectappscreen;

import android.support.v4.app.LoaderManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

/**
 * ListFragment containing all available apps that can be detected by the accessibility service
 */
public class DetectableAppsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<ArrayList<String>> {

    /** Adapter to be used for the list view */
    private ArrayAdapter<String> adapter;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Progress bar while the list loads
        ProgressBar progressBar = new ProgressBar(getActivity());
        progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        getListView().setEmptyView(progressBar);
        // Add progress bar to root of layout
        ViewGroup root = (ViewGroup)getActivity().findViewById(android.R.id.content);
        root.addView(progressBar);


        this.adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        setListAdapter(this.adapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detectable_app_list, container, false);
    }

    @Override
    public Loader<ArrayList<String>> onCreateLoader(int id, Bundle args) {
        return new FileListLoader(getActivity(), "DetectAppScreen");
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<String>> loader, ArrayList<String> data) {
        this.adapter.clear();
        this.adapter.addAll(data);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<String>> loader) {
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        super.onListItemClick(listView, view, position, id);
        final String item = (String)listView.getItemAtPosition(position);

        Intent intent = new Intent(getActivity(), DetectableAppDetails.class);
        intent.putExtra("appName", item); // TODO: make constant
        startActivity(intent);
    }

}
