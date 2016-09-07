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

package simonlang.coastdove.ui.app_usage_data_details;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import simonlang.coastdove.app_usage.ActivityData;
import simonlang.coastdove.app_usage.ActivityDataEntry;
import simonlang.coastdove.R;
import simonlang.coastdove.utility.Misc;

/**
 * Adapter for app usage data entries, displays the type of entry, the number of consecutive occurrences,
 * the activity during which it occurred, and its content
 */
public class DataEntryListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private LayoutInflater inflater;

    private String appPackageName;
    protected List<ActivityData> activityDataList;

    public DataEntryListAdapter(Context context, String appPackageName) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.appPackageName = appPackageName;
        this.activityDataList = new ArrayList<>();
    }

    public DataEntryListAdapter(Context context, String appPackageName, List<ActivityData> activityDataList) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.appPackageName = appPackageName;
        this.activityDataList = activityDataList;
    }

    public void clear() {
        this.activityDataList.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<ActivityData> entries) {
        this.activityDataList.addAll(entries);
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return activityDataList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        ActivityData activityData = activityDataList.get(groupPosition);
        return activityData.getDataEntries().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return activityDataList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        ActivityData activityData = activityDataList.get(groupPosition);
        return activityData.getDataEntries().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        // Use timestamp as ID
        ActivityData activityData = activityDataList.get(groupPosition);
        return activityData.getTimestamp().getTime();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        // Use timestamp as ID
        ActivityData activityData = activityDataList.get(groupPosition);
        ActivityDataEntry entry = activityData.getDataEntries().get(childPosition);
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
            convertView = this.inflater.inflate(R.layout.list_item_activity_data, parent, false);

            holder.innerContainer = (LinearLayout)convertView.findViewById(R.id.activity_data_inner_container);
            holder.activityContent = (TextView)convertView.findViewById(R.id.activity_content);
            holder.activityDuration = (TextView)convertView.findViewById(R.id.activity_duration);

            convertView.setTag(holder);
        }
        else
            holder = (GroupViewHolder)convertView.getTag();

        // Set the actual data
        ActivityData activityData = (ActivityData)getGroup(groupPosition);

        holder.innerContainer.setPadding(activityData.getLevel()*32, 0, 0, 0);
        holder.activityContent.setText(activityData.getShortenedActivity());
        holder.activityDuration.setText(Misc.msToDurationString(activityData.getDuration()));

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
        ActivityData activityData = (ActivityData)getGroup(groupPosition);
        ActivityDataEntry dataEntry = (ActivityDataEntry)getChild(groupPosition, childPosition);

        holder.innerContainer.setPadding(activityData.getLevel()*32, 0, 0, 0);
        holder.entryType.setText(dataEntry.getTypePretty());
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
        TextView activityDuration;
    }

    private class ChildViewHolder {
        LinearLayout innerContainer;
        TextView entryType;
        TextView entryCount;
        TextView entryContent;
    }
}
