package de.uni_bonn.detectappscreen.ui.detectable_app_details;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.util.Pair;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import de.uni_bonn.detectappscreen.R;

/**
 * Adapter for app usage data - an array adapter that tracks which items are selected
 */
public class AppUsageDataListAdapter extends ArrayAdapter<Pair<Integer, String>> {
    private SparseBooleanArray selectedIDs;
    private int resource;

    public AppUsageDataListAdapter(Context context, int resource) {
        super(context, resource);
        this.resource = resource;
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

    public List<Pair<Integer, String>> getSelectedItems() {
        List<Pair<Integer, String>> result = new LinkedList<>();
        for (int i = 0; i < selectedIDs.size(); ++i) {
            if (selectedIDs.valueAt(i)) {
                result.add(getItem(selectedIDs.keyAt(i)));
            }
        }
        return result;
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

        Pair<Integer, String> item = getItem(position);
        TextView textView = (TextView)view; // No custom view, so this view is a TextView
        textView.setText(item.second);

        return view;
    }
}
