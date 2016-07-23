package de.uni_bonn.detectappscreen.detection;

import android.content.Context;

import org.json.JSONObject;

import java.util.List;

import de.uni_bonn.detectappscreen.utility.FileHelper;

/**
 * Loader for app detection data that is supposed to be run in a separate thread
 */
public class AppDetectionDataLoader implements Runnable {

    /** Package name associated with the app detection data */
    private String packageName;
    /** List to add the loaded object to */
    private List<AppDetectionData> detectableAppsLoaded;
    /** Whether to compare current layouts with layout definitions */
    private boolean performLayoutChecks;
    /** Whether to listen to OnClick events */
    private boolean performOnClickChecks;

    /** Application context */
    private Context context;

    /**
     * Constructs a loader using the given data
     * @param packageName             Name of the package associated
     * @param detectableAppsLoaded    List to add the loaded object to
     */
    public AppDetectionDataLoader(String packageName, List<AppDetectionData> detectableAppsLoaded,
                                  boolean performLayoutChecks, boolean performOnClickChecks,
                                  Context context) {
        super();
        this.packageName = packageName;
        this.detectableAppsLoaded = detectableAppsLoaded;
        this.performLayoutChecks = performLayoutChecks;
        this.performOnClickChecks = performOnClickChecks;
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
            layouts = FileHelper.readJSONFile(packageName, "layouts.json");
            reverseMap = FileHelper.readJSONFile(packageName, "reverseMap.json");
        }
        AppDetectionData detectableApp = new AppDetectionData(this.packageName, layouts, reverseMap, context);

        detectableApp.load(this.performLayoutChecks, this.performOnClickChecks);
        synchronized (DetectAppScreenAccessibilityService.detectableAppsLoadedLock) {
            if (detectableApp.isFinishedLoading())
                detectableAppsLoaded.add(detectableApp);
        }
        DetectAppScreenAccessibilityService.onDetectionDataLoadFinished(this.packageName);
    }
}
