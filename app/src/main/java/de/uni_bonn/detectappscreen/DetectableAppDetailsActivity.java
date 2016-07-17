package de.uni_bonn.detectappscreen;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

/**
 * Activity started when a detectable app in the main list is clicked,
 * displays a button to activate/deactivate detection, as well as a list
 * of detection sessions
 */
public class DetectableAppDetailsActivity extends AppCompatActivity {

    /** Package name of the app to be detected */
    private String appPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detectable_app_details);

        // Get package name
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null)
                this.appPackageName = null;
            else
                this.appPackageName = extras.getString(getString(R.string.extras_package_name));
        }
        else
            this.appPackageName = (String)savedInstanceState.getSerializable(getString(R.string.extras_package_name));


        // Set support action bar
        Toolbar toolbar = (Toolbar)findViewById(R.id.detectable_app_toolbar);
        toolbar.setTitle(this.appPackageName);
        setSupportActionBar(toolbar);

        // Switch to activate detection of the specified app
        boolean detectionDataLoadedOrLoading = false;
        try {
            detectionDataLoadedOrLoading = DetectAppScreenAccessibilityService.isDetectionDataLoadedOrLoading(this.appPackageName);
        } catch (NullPointerException e) {
        }
        final Switch activateSwitch = (Switch)findViewById(R.id.detectable_app_activate_switch);
        activateSwitch.setChecked(detectionDataLoadedOrLoading);
        activateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Check whether to load layout and click detection
                    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
                    boolean detectLayouts = preferences.getBoolean(appPackageName + getString(R.string.pref_detect_layouts), false);
                    boolean detectClicks = preferences.getBoolean(appPackageName + getString(R.string.pref_detect_clicks), false);
                    DetectAppScreenAccessibilityService.startLoadingDetectionData(appPackageName, detectLayouts, detectClicks, getApplicationContext());
                }
                else
                    DetectAppScreenAccessibilityService.removeDetectionData(appPackageName);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detectable_app_details_menu, menu);

        // Set up the menu checkboxes
        setUpCheckbox(menu.findItem(R.id.checkbox_detect_layouts), getString(R.string.pref_detect_layouts));
        setUpCheckbox(menu.findItem(R.id.checkbox_detect_clicks), getString(R.string.pref_detect_clicks));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem checkboxDetectLayouts = menu.findItem(R.id.checkbox_detect_layouts);
        MenuItem checkboxDetectClicks = menu.findItem(R.id.checkbox_detect_clicks);
        MenuItem itemDeleteCache = menu.findItem(R.id.item_delete_cache);
        boolean cacheExists = FileHelper.fileExists(appPackageName, "layoutsMap.bin") && FileHelper.fileExists(appPackageName, "reverseMap.bin");

        // If the detection data is currently in use, the menu items are disabled
        boolean detectionDataInUse = DetectAppScreenAccessibilityService.isDetectionDataLoadedOrLoading(appPackageName);
        checkboxDetectLayouts.setEnabled(!detectionDataInUse);
        checkboxDetectClicks.setEnabled(!detectionDataInUse);
        itemDeleteCache.setEnabled(cacheExists && !detectionDataInUse);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Store options regarding layout / click detection
        switch (item.getItemId()) {
            case R.id.checkbox_detect_layouts:
                item.setChecked(!item.isChecked());
                setSharedPreference(appPackageName + getString(R.string.pref_detect_layouts), item.isChecked());
                return true;
            case R.id.checkbox_detect_clicks:
                item.setChecked(!item.isChecked());
                setSharedPreference(appPackageName + getString(R.string.pref_detect_clicks), item.isChecked());
                return true;
            case R.id.item_delete_cache:
                FileHelper.deleteFile(appPackageName, "layoutsMap.bin");
                FileHelper.deleteFile(appPackageName, "reverseMap.bin");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Activates the detectable app specified by the package name
     */
    public void activateDetectableApp(View view) {
        Switch activateSwitch = (Switch)findViewById(R.id.detectable_app_activate_switch);
        activateSwitch.setChecked(!activateSwitch.isChecked());
    }

    /**
     * Returns the package name of the app to be detected
     */
    public String getAppPackageName() {
        return this.appPackageName;
    }

    /**
     * Sets and commits the given preference in this app's SharedPreferences with the given value
     * @param preference    Preference name to set
     * @param value         Desired value of the preference
     */
    private void setSharedPreference(String preference, boolean value) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(preference, value);
        editor.commit();
    }

    /**
     * Sets up this view's menu's checkbox according to the saved preferences
     * @param item          Menu item to set up
     * @param prefString    Name of the preference to look up
     */
    private void setUpCheckbox(MenuItem item, String prefString) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean checked = preferences.getBoolean(appPackageName + prefString, false);
        item.setChecked(checked);
    }
}
