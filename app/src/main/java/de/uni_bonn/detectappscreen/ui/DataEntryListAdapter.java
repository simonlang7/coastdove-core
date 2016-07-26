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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.uni_bonn.detectappscreen.app_usage.AppUsageDataEntry;
import de.uni_bonn.detectappscreen.R;

/**
 * Adapter for app usage data entries, displays the type of entry, the number of consecutive occurrences,
 * the activity during which it occurred, and its content
 */
public class DataEntryListAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater inflater;

    private String appPackageName;
    protected List<AppUsageDataEntry> dataEntries;

    public DataEntryListAdapter(Context context, String appPackageName) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.appPackageName = appPackageName;
        this.dataEntries = new ArrayList<>();
    }

    public DataEntryListAdapter(Context context, String appPackageName, List<AppUsageDataEntry> dataEntries) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.appPackageName = appPackageName;
        this.dataEntries = dataEntries;
    }

    public void clear() {
        this.dataEntries.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<AppUsageDataEntry> entries) {
        this.dataEntries.addAll(entries);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return dataEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return dataEntries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return dataEntries.get(position).getTimestamp().getTime();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            // Set up text views
            holder = new ViewHolder();
            convertView = this.inflater.inflate(R.layout.list_item_data_entry, parent, false);

            holder.entryType = (TextView)convertView.findViewById(R.id.entryType);
            holder.entryCount = (TextView)convertView.findViewById(R.id.entryCount);
            holder.entryActivity = (TextView)convertView.findViewById(R.id.entryActivity);
            holder.entryContent = (TextView)convertView.findViewById(R.id.entryContent);

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        // Set the actual data
        AppUsageDataEntry dataEntry = dataEntries.get(position);

        holder.entryType.setText(dataEntry.getType());
        holder.entryCount.setText("(" + dataEntry.getCount() + "x)");
        holder.entryActivity.setText(dataEntry.getActivity().replace(this.appPackageName + "/", ""));
        holder.entryContent.setText(dataEntry.getContent());

        return convertView;
    }

    private class ViewHolder {
        TextView entryType;
        TextView entryCount;
        TextView entryActivity;
        TextView entryContent;
    }
}
