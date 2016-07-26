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
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.util.ArrayList;

import de.uni_bonn.detectappscreen.R;

/**
 * ListFragment that displays a progress bar while loading its contents
 */
public abstract class FileListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<ArrayList<String>> {

    /** Adapter to be used for the list view */
    protected ArrayAdapter<String> adapter;

    protected ProgressBar progressBar;

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

        this.adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
        setListAdapter(this.adapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_list, container, false);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<String>> loader, ArrayList<String> data) {
        this.adapter.clear();
        this.adapter.addAll(data);
        this.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<String>> loader) {
    }

    protected abstract void addProgressBarToViewGroup();
}
