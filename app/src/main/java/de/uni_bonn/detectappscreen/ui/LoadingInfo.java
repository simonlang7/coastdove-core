package de.uni_bonn.detectappscreen.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;

/**
 * UI elements for displaying progress
 * TODO: overhaul class or re-implement
 */
public class LoadingInfo {
    private String origin;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private int uid;
    private volatile boolean indeterminate;
    private volatile String title;
    private volatile String contentText;
    private volatile int progress;
    private volatile int maxProgress;
    private volatile boolean started;
    private volatile boolean finished;

    private Activity activity;
    private ArrayAdapter listAdapter;
    private ProgressBar progressBar;

    public LoadingInfo(Context context, int uid, String origin) {
        this.origin = origin;
        this.uid = uid;
        initNotification(context);
        this.progress = 0;
        this.maxProgress = 0;
        this.indeterminate = true;
        this.title = "";
        this.contentText = "";
        this.started = false;
        this.finished = false;
    }

    public LoadingInfo(String origin) {
        this.origin = origin;
        this.notificationManager = null;
        this.builder = null;
        this.progressBar = null;
        this.progress = 0;
        this.maxProgress = 0;
        this.indeterminate = true;
        this.title = "";
        this.contentText = "";
        this.started = false;
        this.finished = false;
    }

    public void clearUIElements() {
        synchronized (this) {
            this.activity = null;
            this.listAdapter = null;
            this.progressBar = null;
        }
    }

    public void setUIElements(Activity activity, ArrayAdapter listAdapter) {
        synchronized (this) {
            this.activity = activity;
            this.listAdapter = listAdapter;
            if (started)
                this.listAdapter.notifyDataSetChanged();
        }
    }

    public void setUIElements(Activity activity, ProgressBar progressBar) {
        synchronized (this) {
            this.activity = activity;
            this.progressBar = progressBar;
            if (started) {
                // Todo: un-hardcore
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
            }
        }
    }

    public void setUIElements(Activity activity, ArrayAdapter listAdapter, ProgressBar progressBar) {
        synchronized (this) {
            this.activity = activity;
            this.listAdapter = listAdapter;
            this.progressBar = progressBar;
        }
    }


    public void cancel() {
        if (notificationManager != null)
            notificationManager.cancel(this.uid);
        synchronized (this) {
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finished = true;
                        if (progressBar != null) {
                            progressBar.setIndeterminate(false);
                            progressBar.setVisibility(View.GONE);
                        }
                        if (listAdapter != null) {
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }
    }

    public void start(boolean indeterminate) {
        if (builder != null && indeterminate)
            builder.setProgress(0, 0, true);
        synchronized (this) {
            this.started = true;
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setIndeterminate(true);
                        }
                    }
                });
            }
        }
        update();
        this.progress = 0;
        this.maxProgress = 0;
        this.finished = false;
    }

    public void update(final int maxProgress, final int progress) {
        if (builder != null)
            builder.setProgress(maxProgress, progress, false);
        synchronized (this) {
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressBar != null) {
                            progressBar.setIndeterminate(false);
                            progressBar.setMax(maxProgress);
                            progressBar.setProgress(progress);
                        }
                    }
                });
            }
        }
        this.progress = progress;
        this.maxProgress = maxProgress;
        update();
    }

    public void update() {
        if (notificationManager != null)
            notificationManager.notify(this.uid, builder.build());
        synchronized (this) {
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (listAdapter != null)
                            listAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    public void end() {
        this.finished = true;
        if (builder != null)
            builder.setProgress(0, 0, false);
        synchronized (this) {
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressBar != null) {
                            progressBar.setIndeterminate(false);
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
            }
            this.started = false;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public String getOrigin() {
        return origin;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
    }

    private void initNotification(Context context) {
        this.notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.builder = new NotificationCompat.Builder(context);
    }
}
