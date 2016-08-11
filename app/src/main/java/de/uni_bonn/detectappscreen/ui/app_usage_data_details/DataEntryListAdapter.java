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

package de.uni_bonn.detectappscreen.ui.app_usage_data_details;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.uni_bonn.detectappscreen.analyze.MetaEntry;
import de.uni_bonn.detectappscreen.app_usage.AppUsageDataEntry;
import de.uni_bonn.detectappscreen.R;

/**
 * Adapter for app usage data entries, displays the type of entry, the number of consecutive occurrences,
 * the activity during which it occurred, and its content
 */
public class DataEntryListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private LayoutInflater inflater;

    private String appPackageName;
    protected List<MetaEntry> metaEntries;

    public DataEntryListAdapter(Context context, String appPackageName) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.appPackageName = appPackageName;
        this.metaEntries = new ArrayList<>();
    }

    public DataEntryListAdapter(Context context, String appPackageName, List<MetaEntry> metaEntries) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.appPackageName = appPackageName;
        this.metaEntries = metaEntries;
    }

    public void clear() {
        this.metaEntries.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<MetaEntry> entries) {
        this.metaEntries.addAll(entries);
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return metaEntries.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        MetaEntry metaEntry = metaEntries.get(groupPosition);
        return metaEntry.getActivityData().getDataEntries().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return metaEntries.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        MetaEntry metaEntry = metaEntries.get(groupPosition);
        return metaEntry.getActivityData().getDataEntries().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        // Use timestamp as ID
        MetaEntry metaEntry = metaEntries.get(groupPosition);
        return metaEntry.getActivityData().getTimestamp().getTime();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        // Use timestamp as ID
        MetaEntry metaEntry = metaEntries.get(groupPosition);
        AppUsageDataEntry entry = metaEntry.getActivityData().getDataEntries().get(childPosition);
        return entry.getTimestamp().getTime();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder holder;
        if (convertView == null) {
            // Set up text views
            holder = new GroupViewHolder();
            convertView = this.inflater.inflate(R.layout.list_item_meta_entry, parent, false);

            holder.innerContainer = (LinearLayout)convertView.findViewById(R.id.metaEntryInnerContainer);
            holder.activityContent = (TextView)convertView.findViewById(R.id.activity_content);

            convertView.setTag(holder);
        }
        else
            holder = (GroupViewHolder)convertView.getTag();

        // Set the actual data
        MetaEntry metaEntry = (MetaEntry)getGroup(groupPosition);

        holder.innerContainer.setPadding(metaEntry.getLevel()*32, 0, 0, 0);
        holder.activityContent.setText(metaEntry.getActivityData().getShortenedActivity());

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildViewHolder holder;
        if (convertView == null) {
            // Set up text views
            holder = new ChildViewHolder();
            convertView = this.inflater.inflate(R.layout.list_item_data_entry, parent, false);

            holder.innerContainer = (LinearLayout)convertView.findViewById(R.id.dataEntryInnerContainer);
            holder.entryType = (TextView)convertView.findViewById(R.id.entryType);
            holder.entryCount = (TextView)convertView.findViewById(R.id.entryCount);
            holder.entryContent = (TextView)convertView.findViewById(R.id.entryContent);

            convertView.setTag(holder);
        }
        else
            holder = (ChildViewHolder)convertView.getTag();

        // Set the actual data
        MetaEntry metaEntry = (MetaEntry)getGroup(groupPosition);
        AppUsageDataEntry dataEntry = (AppUsageDataEntry)getChild(groupPosition, childPosition);

        holder.innerContainer.setPadding(metaEntry.getLevel()*32, 0, 0, 0);
        holder.entryType.setText(dataEntry.getType());
        holder.entryCount.setText("(" + dataEntry.getCount() + "x)");
        holder.entryContent.setText(dataEntry.getContent());

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    private class GroupViewHolder {
        LinearLayout innerContainer;
        TextView activityContent;
    }

    private class ChildViewHolder {
        LinearLayout innerContainer;
        TextView entryType;
        TextView entryCount;
        TextView entryContent;
    }
}
