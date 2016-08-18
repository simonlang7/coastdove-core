package de.uni_bonn.detectappscreen.ui;

import android.app.NotificationManager;
import android.graphics.drawable.Drawable;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.ProgressBar;

/**
 * UI elements for displaying progress
 */
public class LoadingInfo {
    public NotificationManager notificationManager;
    public NotificationCompat.Builder builder;
    public ProgressBar progressBar;
    public int uid;

    public LoadingInfo() {
    }

    public void cancel() {
        if (notificationManager != null)
            notificationManager.cancel(this.uid);
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE);
        }
    }

    public void start(boolean indeterminate) {
        if (builder != null && indeterminate)
            builder.setProgress(0, 0, true);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(true);
        }
        update();
    }

    public void update(int maxProgress, int progress) {
        if (builder != null)
            builder.setProgress(maxProgress, progress, false);
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setMax(maxProgress);
            progressBar.setProgress(progress);
        }
        update();
    }

    public void update() {
        if (notificationManager != null)
            notificationManager.notify(this.uid, builder.build());
    }

    public void end() {
        if (builder != null)
            builder.setProgress(0, 0, false);
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE);
        }
        update();
    }

    public void setNotificationData(String title, String contentText, Integer smallIcon) {
        if (builder != null) {
            if (title != null)
                builder.setContentTitle(title);
            if (contentText != null)
                builder.setContentText(contentText);
            if (smallIcon != null)
                builder.setSmallIcon(smallIcon);
        }
    }
}
