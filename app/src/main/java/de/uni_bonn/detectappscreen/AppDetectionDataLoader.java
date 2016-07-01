package de.uni_bonn.detectappscreen;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by Slang on 24.06.2016.
 */
public class AppDetectionDataLoader implements Runnable {

    private String packageName;
    private List<AppDetectionData> detectableAppsLoaded;

    public AppDetectionDataLoader(String packageName, List<AppDetectionData> detectableAppsLoaded) {
        super();
        this.packageName = packageName;
        this.detectableAppsLoaded = detectableAppsLoaded;
    }

    @Override
    public void run() {
        JSONObject layouts;
        JSONObject reverseMap;
        synchronized (DetectAppScreenAccessibilityService.detectableAppsLoadedLock) {
            layouts = AppDetectionData.readJSONFile(packageName, "layouts.json");
            reverseMap = AppDetectionData.readJSONFile(packageName, "reverseMap.json");
        }
        AppDetectionData detectableApp = new AppDetectionData(this.packageName, layouts, reverseMap);
        detectableApp.load();
        synchronized (DetectAppScreenAccessibilityService.detectableAppsLoadedLock) {
            if (detectableApp.isFinishedLoading())
                detectableAppsLoaded.add(detectableApp);
        }
        DetectAppScreenAccessibilityService.onDetectionDataLoadFinished(this.packageName);
    }
}
