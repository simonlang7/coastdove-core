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

    /**
     * Meta data for app usage data entries
     */
    public List<MetaEntry> getAppUsageMetaData() {
        if (this.metaEntries == null)
            initMetaEntries();

        return this.metaEntries;
    }

    /**
     * Initializes the app's meta information by reading it from file, if it exists, or creating an empty
     * one if not
     */
    private void initAppMetaInformation() {
        if (FileHelper.fileExists(appPackageName, "appInformation.json")) {
            JSONObject appMetaInformationJSON = FileHelper.readJSONFile(appPackageName, "appInformation.json");
            this.appMetaInformation = new AppMetaInformation(appMetaInformationJSON);
        }
        else
            this.appMetaInformation = new AppMetaInformation(appPackageName, new LinkedList<String>());
    }

    /**
     * Initializes the list of meta entries
     */
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

        buildMetaEntries(allEntries, firstMainActivityPos);
    }

    /**
     * Builds a list of meta entries from a list of available entries
     * @param allEntries              All entries available
     * @param firstMainActivityPos    Index of the first ActivityDataEntry that contains a MainActivity
     * @return List of meta entries
     */
    private List<MetaEntry> buildMetaEntries(List<AppUsageDataEntry> allEntries, int firstMainActivityPos) {
        List<MetaEntry> result = new LinkedList<>();
        LinkedList<String> activities = new LinkedList<>();
        String mainActivity = allEntries.get(firstMainActivityPos).getShortenedActivity();
        activities.push(mainActivity);

        // go backwards
        int localEndPos = firstMainActivityPos;
        int localStartPos = findPreviousActivityEntry(allEntries, localEndPos);
        while (localStartPos >= 0) {
            addMetaEntry(result, allEntries, activities, localStartPos, localEndPos);

            localEndPos = localStartPos;
            localStartPos = findPreviousActivityEntry(allEntries, localEndPos);
        }

        // go forward
        activities.clear();
        localStartPos = firstMainActivityPos;
        localEndPos = findNextActivityEntry(allEntries, localStartPos);
        while (localEndPos < allEntries.size()) {
            addMetaEntry(result, allEntries, activities, localStartPos, localEndPos);

            localStartPos = localEndPos;
            localEndPos = findNextActivityEntry(allEntries, localStartPos);
        }

        return null;
    }

    /**
     * Creates a MetaEntry and adds it to a given list of meta entries.
     * @param metaEntryList    List to add the meta entry to
     * @param allEntries       All data entries available
     * @param activityStack    Stack of activities found while traversing allEntries
     * @param startPos         Index of the first entry to add to the meta entry
     * @param endPos           Index of the last entry to add to the meta entry
     */
    private void addMetaEntry(List<MetaEntry> metaEntryList, List<AppUsageDataEntry> allEntries, LinkedList<String> activityStack,
                              int startPos, int endPos) {
        AppUsageDataEntry entry = allEntries.get(startPos);
        String activity = entry.getShortenedActivity();
        boolean containsActivity = activityStack.contains(activity);
        if (containsActivity) {
            // Pop until activity removed
            while (!activityStack.pop().equals(activity));
        }
        MetaEntry metaEntry = new MetaEntry(allEntries, activityStack.size(), startPos, endPos);
        metaEntryList.add(metaEntry);
        if (!containsActivity)
            activityStack.push(activity);
    }

    /**
     * Given a list of data entries, this function finds the previous ActivityDataEntry from a given index,
     * and returns the index of the entry found. If there is an ActivityDataEntry at the given position,
     * it is not regarded.
     * @param allEntries      List to search
     * @param fromEntryPos    Position of the entry from which to start.
     * @return Index of the previous ActivityDataEntry, or -1 if none exists
     */
    private int findPreviousActivityEntry(List<AppUsageDataEntry> allEntries, int fromEntryPos) {
        int previousEntryPos = fromEntryPos - 1;
        while (previousEntryPos > 0) {
            AppUsageDataEntry entry = allEntries.get(previousEntryPos);
            if (entry instanceof ActivityDataEntry)
                break;
            --previousEntryPos;
        }
        return previousEntryPos;
    }

    /**
     * Given a list of data entries, this function finds the next ActivityDataEntry from a given index,
     * and returns the index of the entry found. If there is an ActivityDataEntry at the given position,
     * it is not regarded.
     * @param allEntries      List to search
     * @param fromEntryPos    Position of the entry from which to start.
     * @return Index of the next ActivityDataEntry, or allEntries.size() if none exists
     */
    private int findNextActivityEntry(List<AppUsageDataEntry> allEntries, int fromEntryPos) {
        int nextEntryPos = fromEntryPos + 1;
        while (nextEntryPos < allEntries.size()) {
            AppUsageDataEntry entry = allEntries.get(nextEntryPos);
            if (entry instanceof ActivityDataEntry)
                break;
            ++nextEntryPos;
        }
        return nextEntryPos;
    }
}
