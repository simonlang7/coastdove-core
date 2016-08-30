package de.uni_bonn.detectappscreen.app_usage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.uni_bonn.detectappscreen.detection.AppMetaInformation;

/**
 * Processes activity data by trying to deduct an activity hierarchy
 */
public class AppUsageDataProcessor {
    /** Meta information of the app, containing main activity definitions */
    private AppMetaInformation metaInformation;
    /** The session's AppUsageData */
    private AppUsageData appUsageData;

    /**
     * Creates an AppUsageDataProcessor
     * @param metaInformation    Meta information of the app, containing main activity definitions
     * @param appUsageData       AppUsageData to process
     */
    public AppUsageDataProcessor(AppMetaInformation metaInformation, AppUsageData appUsageData) {
        this.metaInformation = metaInformation;
        this.appUsageData = appUsageData;
    }

    /**
     * Process the AppUsageData by adding level information to its ActivityData objects
     */
    public void process() {
        List<ActivityData> activityDataList = new ArrayList<>(this.appUsageData.getActivityDataList());
        int firstMainActivityIndex = findFirstMainActivityIndex(activityDataList);
        deductLevels(activityDataList, firstMainActivityIndex);
    }

    private int findFirstMainActivityIndex(List<ActivityData> activityDataList) {
        int firstMainActivityIndex = 0;
        for (int i = 0; i < activityDataList.size(); ++i) {
            if (this.metaInformation.isMainActivity(activityDataList.get(i))) {
                firstMainActivityIndex = i;
                break;
            }
        }
        return firstMainActivityIndex;
    }

    private void deductLevels(List<ActivityData> activityDataList, int firstMainActivityIndex) {
        LinkedList<String> activityStack = new LinkedList<>();
        String mainActivity = activityDataList.get(firstMainActivityIndex).getShortenedActivity();
        activityStack.push(mainActivity);

        // go backwards
        for (int i = firstMainActivityIndex - 1; i >= 0; --i)
            setLevel(activityDataList.get(i), activityStack);

        // go forward
        activityStack.clear();
        for (int i = firstMainActivityIndex; i < activityDataList.size(); ++i)
            setLevel(activityDataList.get(i), activityStack);
    }

    private void setLevel(ActivityData activityData, LinkedList<String> activityStack) {
        String activity = activityData.getShortenedActivity();
        boolean containsActivity = activityStack.contains(activity);
        if (containsActivity) {
            // Pop until activity removed
            String topActivity = activityStack.pop();
            while (!topActivity.equals(activity))
                topActivity = activityStack.pop();
        }
        // Encountered another main activity? Must be level 0.
        if (this.metaInformation.isMainActivity(activityData))
            activityStack.clear();

        activityData.setLevel(activityStack.size());
        activityStack.push(activity);
    }
}
