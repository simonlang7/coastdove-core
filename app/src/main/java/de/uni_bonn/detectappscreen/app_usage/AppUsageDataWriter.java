/*  DetectAppScreen
    Copyright (C) 2016  Simon Lang
    Contact: simon.lang7 at gmail dot com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package de.uni_bonn.detectappscreen.app_usage;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import de.uni_bonn.detectappscreen.utility.FileHelper;

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
            FileHelper.scanFile(this.context, file.getAbsolutePath());
        } catch (JSONException e) {
            Log.e("AppUsageDataWriter", "Error writing JSONObject for " + data.getPackageName() + ": " + e.getMessage());
        } catch (IOException e) {
            Log.e("AppUsageDataWriter", "Input/Output error for " + data.getPackageName() + ": " + e.getMessage());
        }
    }
}
