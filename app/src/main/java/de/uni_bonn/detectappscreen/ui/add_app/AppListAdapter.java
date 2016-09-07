package de.uni_bonn.detectappscreen.ui.add_app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = this.inflater.inflate(R.layout.list_item_app, parent, false);

            holder.imageView = (ImageView)convertView.findViewById(R.id.appIcon);
            holder.appName = (TextView)convertView.findViewById(R.id.appName);
            holder.progressBar = (ProgressBar)convertView.findViewById(R.id.appProgressBar);
            holder.loadingElements = (LinearLayout)convertView.findViewById(R.id.loadingElements);
            holder.loadingTitle = (TextView)convertView.findViewById(R.id.loadingTitle);
            holder.loadingContentText = (TextView)convertView.findViewById(R.id.loadingContentText);
            holder.cancelButton = (ImageButton)convertView.findViewById(R.id.cancelButton);

            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        // Set up image and text
        PackageManager pm = getContext().getPackageManager();
        final ApplicationInfo appInfo = getItem(position);
        holder.imageView.setImageDrawable(appInfo.loadIcon(pm));
        String text = appInfo.loadLabel(pm).toString();
        holder.appName.setText(text);

        // Update progress bar
        final MultipleObjectLoader<AppDetectionData> multiLoader = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader();
        LoadingInfo loadingInfo = multiLoader.getLoadingInfo(appInfo.packageName);
        ProgressBar progressBar = holder.progressBar;
        if (loadingInfo != null) {
            if (!loadingInfo.isFinished()) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(loadingInfo.isIndeterminate());
                progressBar.setMax(loadingInfo.getMaxProgress());
                progressBar.setProgress(loadingInfo.getProgress());

                holder.loadingElements.setVisibility(View.VISIBLE);

                if (multiLoader.isInterrupted(appInfo.packageName)) {
                    holder.loadingTitle.setText(getContext().getString(R.string.add_app_cancelling));
                    holder.loadingContentText.setText("");
                }
                else {
                    holder.loadingTitle.setText(loadingInfo.getTitle());
                    holder.loadingContentText.setText(loadingInfo.getContentText());
                }

                holder.cancelButton.setVisibility(View.VISIBLE);
                holder.cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        multiLoader.interrupt(appInfo.packageName);
                        notifyDataSetChanged();
                    }
                });
            }
        }
        if (loadingInfo == null || loadingInfo.isFinished()) {
            progressBar.setVisibility(View.GONE);
            progressBar.setIndeterminate(false);

            holder.loadingElements.setVisibility(View.GONE);
            holder.loadingTitle.setText("");
            holder.loadingContentText.setText("");
            holder.cancelButton.setVisibility(View.GONE);
            holder.cancelButton.setOnClickListener(null);
        }

        // Text: black if cache exists, gray otherwise
        if (FileHelper.appDetectionDataExists(getContext(), appInfo.packageName) || loadingInfo != null)
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
        LinearLayout loadingElements;
        TextView loadingTitle;
        TextView loadingContentText;
        ImageButton cancelButton;
    }
}
