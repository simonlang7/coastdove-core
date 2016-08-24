package de.uni_bonn.detectappscreen.ui.add_app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.setup.LayoutCollection;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.utility.MultipleObjectLoader;

/**
 * Adapter for app usage data - an array adapter that tracks which items are selected
 */
public class AppListAdapter extends ArrayAdapter<ApplicationInfo> {

    private LayoutInflater inflater;

    public AppListAdapter(Context context, int resource) {
        super(context, resource);
        this.inflater = LayoutInflater.from(getContext());
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = this.inflater.inflate(R.layout.list_item_app, parent, false);

            holder.imageView = (ImageView)convertView.findViewById(R.id.appIcon);
            holder.appName = (TextView)convertView.findViewById(R.id.appName);
//            holder.progressBar = (ProgressBar)convertView.findViewById(R.id.appProgressBar);

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        PackageManager pm = getContext().getPackageManager();
        ApplicationInfo appInfo = getItem(position);
        holder.imageView.setImageDrawable(appInfo.loadIcon(pm));
        String text = appInfo.loadLabel(pm).toString();
        holder.appName.setText(text);

//        MultipleObjectLoader<LayoutCollection> multiLoader = LayoutCollection.getLayoutCollectionMultipleObjectLoader();
//        LoadingInfo loadingInfo = multiLoader.getLoadingInfo(appInfo.packageName);
//        if (loadingInfo != null) {
//            ProgressBar progressBar = holder.progressBar;
//            if (!loadingInfo.isFinished() && loadingInfo.isUpdated()) {
//                progressBar.setVisibility(View.VISIBLE);
//                progressBar.setIndeterminate(loadingInfo.getMaxProgress() == 0);
//                progressBar.setMax(loadingInfo.getMaxProgress());
//                progressBar.setProgress(loadingInfo.getProgress());
//            }
//            else if (loadingInfo.isFinished()) {
//                progressBar.setVisibility(View.GONE);
//                progressBar.setIndeterminate(false);
//                progressBar.setProgress(loadingInfo.getMaxProgress());
//                progressBar.setMax(loadingInfo.getMaxProgress());
//            }
//        }

        return convertView;
    }


    private class ViewHolder {
        ImageView imageView;
        TextView appName;
//        ProgressBar progressBar;
    }
}
