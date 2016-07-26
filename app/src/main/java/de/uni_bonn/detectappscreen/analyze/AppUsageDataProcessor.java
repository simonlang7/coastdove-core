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
    private List<MetaEntry> metaEntries;

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
        this.metaEntries = null;
        initAppMetaInformation();
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
        this.metaEntries = null;
        initAppMetaInformation();
    }

    public List<MetaEntry> getAppUsageMetaData() {
        if (this.metaEntries == null)
            initMetaEntries();

        return this.metaEntries;
    }

    private void initAppMetaInformation() {
        if (FileHelper.fileExists(appPackageName, "appInformation.json")) {
            JSONObject appMetaInformationJSON = FileHelper.readJSONFile(appPackageName, "appInformation.json");
            this.appMetaInformation = new AppMetaInformation(appMetaInformationJSON);
        }
        else
            this.appMetaInformation = new AppMetaInformation(appPackageName, new LinkedList<String>());
    }

    private void initMetaEntries() {
        this.metaEntries = new LinkedList<>();

        List<AppUsageDataEntry> allEntries = new ArrayList<>(this.appUsageData.getDataEntries());
        int firstMainActivityPos = 0;
        while (!this.appMetaInformation.isMainActivity(allEntries.get(firstMainActivityPos))) {
            firstMainActivityPos = findNextActivityEntry(allEntries, firstMainActivityPos);
            if (firstMainActivityPos >= allEntries.size()) {
                firstMainActivityPos = 0;
                break;
            }
        }



    }

    private List<MetaEntry> buildMetaEntries(List<AppUsageDataEntry> allEntries, boolean backwards, int startPos) {
        LinkedList<String> activities = new LinkedList<>();

        
    }

    private int findPreviousActivityEntry(List<AppUsageDataEntry> allEntries, int startPos) {
        int endPos = startPos - 1;
        while (endPos > 0) {
            AppUsageDataEntry entry = allEntries.get(endPos);
            if (entry instanceof ActivityDataEntry)
                break;
            --endPos;
        }
        return endPos;
    }

    private int findNextActivityEntry(List<AppUsageDataEntry> allEntries, int startPos) {
        int endPos = startPos + 1;
        while (endPos < allEntries.size()) {
            AppUsageDataEntry entry = allEntries.get(endPos);
            if (entry instanceof ActivityDataEntry)
                break;
            ++endPos;
        }
        return endPos;
    }
}
