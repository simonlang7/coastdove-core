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
import de.uni_bonn.detectappscreen.app_usage.ActivityData;
import de.uni_bonn.detectappscreen.app_usage.AppUsageData;
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
     * @param appUsageData        AppUsageData object to process
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
                context, FileHelper.Directory.APP_USAGE_DATA, appPackageName, filename);
        this.appUsageData = new AppUsageData(appUsageDataJSON);
        this.metaEntries = null;
        initAppMetaInformation();
    }

    /**
     * Meta data for app usage data entries
     */
    public List<MetaEntry> getAppUsageMetaData() {
        if (this.metaEntries == null)
            initMetaEntries();

        return this.metaEntries;
    }

    /**
     * Converts all data contained to strings with appropriate padding
     * @return All ActivityData of the metaEntries as strings
     */
    public String[] toStrings() {
        List<String> strings = new LinkedList<>();
        for (MetaEntry metaEntry : this.metaEntries) {
            int level = metaEntry.getLevel();
            String[] activityData = metaEntry.getActivityData().toStrings(2*level);
            for (String string : activityData) {
                strings.add(string);
            }
        }
        return strings.toArray(new String[strings.size()]);
    }

    /**
     * Initializes the app's meta information by reading it from file, if it exists, or creating an empty
     * one if not
     */
    private void initAppMetaInformation() {
        if (FileHelper.fileExists(appPackageName, "appInformation.json")) {
            JSONObject appMetaInformationJSON = FileHelper.readJSONFile(
                    this.context, FileHelper.Directory.PACKAGE, this.appPackageName, "appInformation.json");
            this.appMetaInformation = new AppMetaInformation(appMetaInformationJSON);
        }
        else
            this.appMetaInformation = new AppMetaInformation(this.appPackageName, new LinkedList<String>());
    }

    /**
     * Initializes the list of meta entries
     */
    private void initMetaEntries() {
        this.metaEntries = new LinkedList<>();

        List<ActivityData> activityDataList = new ArrayList<>(this.appUsageData.getActivityDataList());
        int firstMainActivityIndex = 0;
        for (int i = 0; i < activityDataList.size(); ++i) {
            if (this.appMetaInformation.isMainActivity(activityDataList.get(i))) {
                firstMainActivityIndex = i;
                break;
            }
        }

        this.metaEntries = buildMetaEntries(activityDataList, firstMainActivityIndex);
    }

    /**
     * Builds a list of meta entries from a list of activity data objects
     * @param activityDataList              All activity data objects
     * @param firstMainActivityIndex        Index of the first ActivityData that contains a MainActivity
     * @return List of meta entries
     */
    private List<MetaEntry> buildMetaEntries(List<ActivityData> activityDataList, int firstMainActivityIndex) {
        List<MetaEntry> result = new LinkedList<>();
        LinkedList<String> activities = new LinkedList<>();
        String mainActivity = activityDataList.get(firstMainActivityIndex).getShortenedActivity();
        activities.push(mainActivity);

        // go backwards
        for (int i = firstMainActivityIndex - 1; i >= 0; ++i) {
            addMetaEntry(result, activityDataList, i, activities, true);
        }

        // go forward
        activities.clear();
        for (int i = firstMainActivityIndex; i < activityDataList.size(); ++i) {
            addMetaEntry(result, activityDataList, i, activities, false);
        }

        return result;
    }

    /**
     * Create a MetaEntry and adds it to the given list of meta entries
     * @param metaEntryList       List to add the meta entry to
     * @param activityDataList    Source list to take the activity data from
     * @param index               Index of the activity data to use
     * @param activityStack       Stack of activities found while traversing activityDataList
     * @param putFront            The MetaEntry is added to the front of the list if true,
     *                            or to the back if false
     */
    private void addMetaEntry(List<MetaEntry> metaEntryList, List<ActivityData> activityDataList,
                              int index, LinkedList<String> activityStack, boolean putFront) {
        ActivityData data = activityDataList.get(index);
        String activity = data.getShortenedActivity();
        boolean containsActivity = activityStack.contains(activity);
        if (containsActivity) {
            // Pop until activity removed
            String topActivity = activityStack.pop();
            while (!topActivity.equals(activity))
                topActivity = activityStack.pop();
        }

        MetaEntry metaEntry = new MetaEntry(data, activityStack.size());
        if (putFront)
            metaEntryList.add(0, metaEntry);
        else
            metaEntryList.add(metaEntry);

        activityStack.push(activity);
    }

}
