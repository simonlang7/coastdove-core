package de.uni_bonn.detectappscreen;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Runnable Writer for app usage data, run whenever a detectable app is exited
 * in order to save the collected data
 */
public class AppUsageDataWriter implements Runnable {

    /** Sub-directory to save the collected data in */
    private String subDirectory;
    /** App usage data to save */
    private AppUsageData data;
    /** Application context */
    private Context context;

    /**
     * Creates a new AppUsageDataWriter
     * @param subDirectory    Sub-directory to save the collected data in
     * @param data            App usage data to save
     */
    public AppUsageDataWriter(String subDirectory, AppUsageData data, Context context) {
        this.subDirectory = subDirectory;
        this.data = data;
        this.context = context;
    }

    /**
     * Performs the actual writing of the data
     */
    @Override
    public void run() {
        // todo: outsource "DetectAppScreen"
        File directory = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), data.getPackageName() + "/" + subDirectory + "/");
        directory.mkdirs();
        File file = new File(directory, data.getFilename());
        try (FileWriter fw = new FileWriter(file, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(data.toJSON().toString(4));
            scanFile(file.getAbsolutePath());
        } catch (JSONException e) {
            Log.e("AppUsageDataWriter", "Error writing JSONObject for " + data.getPackageName() + ": " + e.getMessage());
        } catch (IOException e) {
            Log.e("AppUsageDataWriter", "Input/Output error for " + data.getPackageName() + ": " + e.getMessage());
        }
    }

    /**
     * Lets the media scanner scan any files given
     * @param paths Absolute paths of the files to scan
     */
    private void scanFile(String... paths) {
        MediaScannerConnection.scanFile(this.context, paths, null, new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {

            }

            @Override
            public void onScanCompleted(String path, Uri uri) {
                Log.v("Media scanner", "Scanned file: " + path);
            }
        });
    }
}
