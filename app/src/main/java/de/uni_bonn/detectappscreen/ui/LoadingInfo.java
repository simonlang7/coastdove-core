package de.uni_bonn.detectappscreen.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.ProgressBar;

/**
 * UI elements for displaying progress
 */
public class LoadingInfo {
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private ProgressBar progressBar;
    private Activity activity;
    private int uid;

    public LoadingInfo(@NonNull Activity activity, int uid,
                       @NonNull ProgressBar progressBar, boolean notification) {
        this.activity = activity;
        this.uid = uid;
        this.progressBar = progressBar;
        if (notification)
            initNotification();
        else {
            this.notificationManager = null;
            this.builder = null;
        }
    }

    public LoadingInfo(@NonNull Activity activity, int uid) {
        this.activity = activity;
        this.uid = uid;
        this.progressBar = null;
        initNotification();
    }

    public LoadingInfo() {
        this.notificationManager = null;
        this.builder = null;
        this.progressBar = null;
    }

    public void cancel() {
        if (notificationManager != null)
            notificationManager.cancel(this.uid);
        if (progressBar != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    public void start(boolean indeterminate) {
        if (builder != null && indeterminate)
            builder.setProgress(0, 0, true);
        if (progressBar != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(true);
                }
            });
        }
        update();
    }

    public void update(final int maxProgress, final int progress) {
        if (builder != null)
            builder.setProgress(maxProgress, progress, false);
        if (progressBar != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(maxProgress);
                    progressBar.setProgress(progress);
                }
            });
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
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisibility(View.GONE);
                }
            });
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

    private void initNotification() {
        this.notificationManager = (NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
        this.builder = new NotificationCompat.Builder(activity);
    }
}
