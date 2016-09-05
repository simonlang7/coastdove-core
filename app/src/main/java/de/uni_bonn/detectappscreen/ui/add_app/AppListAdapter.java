package de.uni_bonn.detectappscreen.ui.add_app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.detection.AppDetectionData;
import de.uni_bonn.detectappscreen.detection.DetectAppScreenAccessibilityService;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.utility.FileHelper;
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
            holder.progressBar = (ProgressBar)convertView.findViewById(R.id.appProgressBar);

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        // Set up image and text
        PackageManager pm = getContext().getPackageManager();
        ApplicationInfo appInfo = getItem(position);
        holder.imageView.setImageDrawable(appInfo.loadIcon(pm));
        String text = appInfo.loadLabel(pm).toString();
        holder.appName.setText(text);

        // Update progress bar
        MultipleObjectLoader<AppDetectionData> multiLoader = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader();
        LoadingInfo loadingInfo = multiLoader.getLoadingInfo(appInfo.packageName);
        ProgressBar progressBar = holder.progressBar;
        if (loadingInfo != null) {
            if (!loadingInfo.isFinished()) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(loadingInfo.getMaxProgress() == 0);
                progressBar.setMax(loadingInfo.getMaxProgress());
                progressBar.setProgress(loadingInfo.getProgress());
            }
        }
        if (loadingInfo == null || loadingInfo.isFinished()) {
            progressBar.setVisibility(View.GONE);
            progressBar.setIndeterminate(false);
        }

        // Text: black if cache exists, gray otherwise
        if (FileHelper.appDetectionDataExists(getContext(), appInfo.packageName))
            holder.appName.setTextColor(getContext().getResources().
                    getColor(R.color.primary_text_default_material_light));
        else
            holder.appName.setTextColor(getContext().getResources().
                    getColor(R.color.primary_text_disabled_material_light));

        return convertView;
    }


    private class ViewHolder {
        ImageView imageView;
        TextView appName;
        ProgressBar progressBar;
    }
}
