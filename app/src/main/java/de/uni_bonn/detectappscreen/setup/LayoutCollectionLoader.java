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

package de.uni_bonn.detectappscreen.setup;


import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipFile;

import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.utility.FileHelper;
import de.uni_bonn.detectappscreen.utility.MultipleObjectLoader;
import de.uni_bonn.detectappscreen.utility.ObjectLoader;

/**
 * Loads a LayoutCollection
 */
public class LayoutCollectionLoader extends ObjectLoader<LayoutCollection> {
    private Context context;
    private String appPackageName;
    private String apkFullPath;
    private float minDetectionRate;
    private LoadingInfo loadingInfo;

    public LayoutCollectionLoader(Context context, String appPackageName, String apkFullPath,
                                  MultipleObjectLoader<LayoutCollection> multipleObjectLoader,
                                  float minDetectionRate, LoadingInfo loadingInfo) {
        super(appPackageName, multipleObjectLoader);
        this.context = context;
        this.appPackageName = appPackageName;
        this.apkFullPath = apkFullPath;
        this.minDetectionRate = minDetectionRate;
        this.loadingInfo = loadingInfo;
    }

    @Override
    protected LayoutCollection load() {
        File file = new File(apkFullPath);
        LayoutCollection layouts = null;
        try (ZipFile apk = new ZipFile(file)) {
            layouts = new LayoutCollection(apk, appPackageName, minDetectionRate, loadingInfo);
        } catch (IOException e) {
            Log.e("LayoutCollectionLoader", "Unable to load APK: " + e.getMessage());
        }

        if (layouts == null)
            return null;

        String appPackageName = this.appPackageName.replace(".apk", "");

        File layoutsFile = FileHelper.getFile(this.context, FileHelper.Directory.PACKAGE, appPackageName, "layouts.json");
        File parent = layoutsFile.getParentFile();
        parent.mkdir();
        try (FileWriter fw = new FileWriter(layoutsFile, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(layouts.toJSON().toString(4).replaceAll("\\\\", ""));
        } catch (Exception e) {
            Log.e("LayoutCollectionLoader", "Unable to write to JSON: " + e.getMessage());
        }

        File reverseMapFile = FileHelper.getFile(this.context, FileHelper.Directory.PACKAGE, appPackageName, "reverseMap.json");
        try (FileWriter fw = new FileWriter(reverseMapFile, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(layouts.getReverseMap().toJSON().toString(4).replaceAll("\\\\", ""));
        } catch (Exception e) {
            Log.e("LayoutCollectionLoader", "Unable to write to JSON: " + e.getMessage());
        }
        FileHelper.scanFile(context, layoutsFile.getAbsolutePath(), reverseMapFile.getAbsolutePath());

        return layouts;
    }
}
