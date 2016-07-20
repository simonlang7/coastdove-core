package de.uni_bonn.detectappscreen;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

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

//        ((TextView)convertView.findViewById(R.id.entryType)).setText(dataEntry.getType());
//        ((TextView)convertView.findViewById(R.id.entryCount)).setText("(" + dataEntry.getCount() + "x)");
//        ((TextView)convertView.findViewById(R.id.entryActivity)).setText(dataEntry.getActivity());
//        ((TextView)convertView.findViewById(R.id.entryContent)).setText(dataEntry.getContent());

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
