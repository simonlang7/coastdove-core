package de.uni_bonn.detectappscreen;

import android.content.Context;

import org.json.JSONObject;

import java.util.List;

/**
 * Loader for app detection data that is supposed to be run in a separate thread
 */
public class AppDetectionDataLoader implements Runnable {

    /** Package name associated with the app detection data */
    private String packageName;
    /** List to add the loaded object to */
    private List<AppDetectionData> detectableAppsLoaded;
    /** Application context */
    private Context context;

    /**
     * Constructs a loader using the given data
     * @param packageName             Name of the package associated
     * @param detectableAppsLoaded    List to add the loaded object to
     */
    public AppDetectionDataLoader(String packageName, List<AppDetectionData> detectableAppsLoaded, Context context) {
        super();
        this.packageName = packageName;
        this.detectableAppsLoaded = detectableAppsLoaded;
        this.context = context;
    }

    /**
     * Loads the according app detection data
     */
    @Override
    public void run() {
        JSONObject layouts;
        JSONObject reverseMap;
        synchronized (DetectAppScreenAccessibilityService.detectableAppsLoadedLock) {
            layouts = AppDetectionData.readJSONFile(packageName, "layouts.json");
            reverseMap = AppDetectionData.readJSONFile(packageName, "reverseMap.json");
        }
        AppDetectionData detectableApp = new AppDetectionData(this.packageName, layouts, reverseMap, context);
        detectableApp.load();
        synchronized (DetectAppScreenAccessibilityService.detectableAppsLoadedLock) {
            if (detectableApp.isFinishedLoading())
                detectableAppsLoaded.add(detectableApp);
        }
        DetectAppScreenAccessibilityService.onDetectionDataLoadFinished(this.packageName);
    }
}
