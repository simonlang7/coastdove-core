package de.uni_bonn.detectappscreen;

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

    /**
     * Creates a new AppUsageDataWriter
     * @param subDirectory    Sub-directory to save the collected data in
     * @param data            App usage data to save
     */
    public AppUsageDataWriter(String subDirectory, AppUsageData data) {
        this.subDirectory = subDirectory;
        this.data = data;
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
        } catch (JSONException e) {
            Log.e("AppUsageDataWriter", "Error writing JSONObject for " + data.getPackageName() + ": " + e.getMessage());
        } catch (IOException e) {
            Log.e("AppUsageDataWriter", "Input/Output error for " + data.getPackageName() + ": " + e.getMessage());
        }
    }
}
