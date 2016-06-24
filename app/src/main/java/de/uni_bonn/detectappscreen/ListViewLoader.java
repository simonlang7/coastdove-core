package de.uni_bonn.detectappscreen;


import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;


/**
 * Created by Slang on 21.06.2016.
 */
public class ListViewLoader extends ListActivity implements LoaderManager.LoaderCallbacks<ArrayList<String>> {

    private ArrayAdapter<String> adapter;
    private static final String APP_NAME = "DetectAppScreen";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // Progress bar while the list loads
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        getListView().setEmptyView(progressBar);
        // Add progress bar to root of layout
        ViewGroup root = (ViewGroup)findViewById(android.R.id.content);
        root.addView(progressBar);


        this.adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        setListAdapter(this.adapter);

        getLoaderManager().initLoader(0, null, this);
    }

    public void openAccessibilityServices(View view) {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    @Override
    public Loader<ArrayList<String>> onCreateLoader(int id, Bundle args) {
        return new FileListLoader(this, "DetectAppScreen");
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<String>> loader, ArrayList<String> data) {
        this.adapter.clear();
        this.adapter.addAll(data);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<String>> loader) {
        this.adapter.clear();
    }

    @Override
    protected void onListItemClick(ListView listView, final View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        final String item = (String)listView.getItemAtPosition(position);
        view.animate().setDuration(2000).alpha(0).withEndAction(new Runnable() {

            @Override
            public void run() {
                adapter.remove(item);
                view.setAlpha(1);
                DetectAppScreenAccessibilityService.startLoadingDetectionData(item);
            }
        });
    }
}