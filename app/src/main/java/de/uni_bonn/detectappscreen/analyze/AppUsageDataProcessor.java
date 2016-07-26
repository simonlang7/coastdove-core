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

package de.uni_bonn.detectappscreen.analyze;

import android.content.Context;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.app_usage.ActivityDataEntry;
import de.uni_bonn.detectappscreen.app_usage.AppUsageData;
import de.uni_bonn.detectappscreen.app_usage.AppUsageDataEntry;
import de.uni_bonn.detectappscreen.utility.FileHelper;

/**
 * Processes app usage data by formatting it and replacing parts of it for reading convenience
 */
public class AppUsageDataProcessor {
    /** Package name of the app */
    private String appPackageName;
    /** Meta information for the app */
    private AppMetaInformation appMetaInformation;
    /** The session's AppUsageData */
    private AppUsageData appUsageData;
    /** Context */
    private Context context;

    /** Meta data for app usage data entries */
    private List<AppUsageMetaData> appUsageMetaDataList;

    /**
     * Creates an AppUsageDataProcessor with the given parameters
     * @param context             Context that created this object
     * @param appPackageName      The associated app's package name
     * @param appUsageData    AppUsageData object to process
     */
    public AppUsageDataProcessor(Context context, String appPackageName, AppUsageData appUsageData) {
        this.context = context;
        this.appPackageName = appPackageName;
        this.appUsageData = appUsageData;
        this.appUsageMetaDataList = null;
        assignAppMetaInformation();
    }

    /**
     * Creates an AppUsageDataProcessor, reading the AppUsageData from file
     * @param context           Context that created this object
     * @param appPackageName    The associated app's package name
     * @param filename          File from which to load the AppUsageData object
     */
    public AppUsageDataProcessor(Context context, String appPackageName, String filename) {
        this.context = context;
        this.appPackageName = appPackageName;
        JSONObject appUsageDataJSON = FileHelper.readJSONFile(
                this.appPackageName + "/" + this.context.getString(R.string.app_usage_data_folder_name), filename);
        this.appUsageData = new AppUsageData(appUsageDataJSON);
        this.appUsageMetaDataList = null;
        assignAppMetaInformation();
    }

    public List<AppUsageMetaData> getAppUsageMetaData() {
        if (this.appUsageMetaDataList != null)
            return this.appUsageMetaDataList;

        this.appUsageMetaDataList = new LinkedList<>();

        List<AppUsageDataEntry> dataEntries = new ArrayList<>(this.appUsageData.getDataEntries());
        int firstMainActivityPos = 0;
        for (int i = 0; i < dataEntries.size(); ++i) {
            AppUsageDataEntry entry = dataEntries.get(i);
            if (entry instanceof ActivityDataEntry &&
                    this.appMetaInformation.isMainActivity(entry.getShortenedActivity())) {
                firstMainActivityPos = i;
                break;
            }
        }

        

        return this.appUsageMetaDataList;
    }

    private void assignAppMetaInformation() {
        if (FileHelper.fileExists(appPackageName, "appInformation.json")) {
            JSONObject appMetaInformationJSON = FileHelper.readJSONFile(appPackageName, "appInformation.json");
            this.appMetaInformation = new AppMetaInformation(appMetaInformationJSON);
        }
        else
            this.appMetaInformation = new AppMetaInformation(appPackageName, new LinkedList<String>());
    }
}
