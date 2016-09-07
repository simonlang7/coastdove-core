/*  Coast Dove
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

package simonlang.coastdove.ui.detectable_app_details;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import simonlang.coastdove.R;

/**
 * Adapter for app usage data - an array adapter that tracks which items are selected
 */
public class AppUsageDataListAdapter extends ArrayAdapter<AppUsageDataUIContainer> {
    private LayoutInflater inflater;
    private int resource;

    private SparseBooleanArray selectedIDs;

    public AppUsageDataListAdapter(Context context, int resource) {
        super(context, resource);
        this.inflater = LayoutInflater.from(getContext());
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

    public List<AppUsageDataUIContainer> getSelectedItems() {
        List<AppUsageDataUIContainer> result = new LinkedList<>();
        for (int i = 0; i < selectedIDs.size(); ++i) {
            if (selectedIDs.valueAt(i)) {
                result.add(getItem(selectedIDs.keyAt(i)));
            }
        }
        return result;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = this.inflater.inflate(R.layout.list_item_app_usage_data, parent, false);

            holder.timestamp = (TextView)convertView.findViewById(R.id.app_usage_data_timestamp);
            holder.duration = (TextView)convertView.findViewById(R.id.app_usage_data_duration);

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        boolean selected = selectedIDs.get(position, false);
        if (selected) {
            convertView.setSelected(true);
            convertView.setPressed(true);
            convertView.setBackgroundColor(getContext().getResources().getColor(R.color.colorListItemChecked));
        } else {
            convertView.setSelected(false);
            convertView.setPressed(false);
            convertView.setBackgroundColor(Color.WHITE);
        }

        AppUsageDataUIContainer item = getItem(position);
        holder.timestamp.setText(item.timestamp);
        holder.duration.setText(item.duration);

        return convertView;
    }

    private class ViewHolder {
        TextView timestamp;
        TextView duration;
    }
}
