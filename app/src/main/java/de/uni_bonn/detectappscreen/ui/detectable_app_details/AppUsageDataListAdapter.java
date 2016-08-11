package de.uni_bonn.detectappscreen.ui.detectable_app_details;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import de.uni_bonn.detectappscreen.R;

/**
 * Adapter for app usage data - an array adapter that tracks which items are selected
 */
public class AppUsageDataListAdapter extends ArrayAdapter<String> {
    private SparseBooleanArray selectedIDs;

    public AppUsageDataListAdapter(Context context, int resource) {
        super(context, resource);
        selectedIDs = new SparseBooleanArray();
    }

    public void toggleSelected(Integer position) {
        boolean selected = selectedIDs.get(position, false);
        if (selected)
            selectedIDs.delete(position);
        else
            selectedIDs.put(position, true);
    }

    public void resetSelected() {
        selectedIDs.clear();
    }

    public int selectedCount() {
        return selectedIDs.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        boolean selected = selectedIDs.get(position, false);
        if (selected) {
            view.setSelected(true);
            view.setPressed(true);
            view.setBackgroundColor(getContext().getResources().getColor(R.color.colorListItemChecked));
        } else {
            view.setSelected(false);
            view.setPressed(false);
            view.setBackgroundColor(Color.WHITE);
        }
        return view;
    }
}
