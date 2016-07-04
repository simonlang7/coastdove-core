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
 * Created by Slang on 01.07.2016.
 */
public class AppUsageDataWriter implements Runnable {

    private String subDirectory;
    private AppUsageData data;

    public AppUsageDataWriter(String subDirectory, AppUsageData data) {
        this.subDirectory = subDirectory;
        this.data = data;
    }

    @Override
    public void run() {
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
