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
    private String packageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detectable_app_details);

        // Get package name
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null)
                this.packageName = null;
            else
                this.packageName = extras.getString(getString(R.string.extras_package_name));
        }
        else
            this.packageName = (String)savedInstanceState.getSerializable(getString(R.string.extras_package_name));


        // Set support action bar
        Toolbar toolbar = (Toolbar)findViewById(R.id.detectable_app_toolbar);
        toolbar.setTitle(this.packageName);
        setSupportActionBar(toolbar);

        // Switch to activate detection of the specified app
        final Switch activateSwitch = (Switch)findViewById(R.id.detectable_app_activate_switch);
        activateSwitch.setChecked(DetectAppScreenAccessibilityService.isDetectionDataLoadedOrLoading(this.packageName));
        activateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Check whether to load layout and click detection
                    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
                    boolean detectLayouts = preferences.getBoolean(packageName + getString(R.string.pref_detect_layouts), false);
                    boolean detectClicks = preferences.getBoolean(packageName + getString(R.string.pref_detect_clicks), false);
                    DetectAppScreenAccessibilityService.startLoadingDetectionData(packageName, detectLayouts, detectClicks, getApplicationContext());
                }
                else
                    DetectAppScreenAccessibilityService.removeDetectionData(packageName);
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
        boolean cacheExists = FileHelper.fileExists(packageName, "layoutsMap.bin") && FileHelper.fileExists(packageName, "reverseMap.bin");

        // If the detection data is currently in use, the menu items are disabled
        boolean detectionDataInUse = DetectAppScreenAccessibilityService.isDetectionDataLoadedOrLoading(packageName);
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
                setSharedPreference(packageName + getString(R.string.pref_detect_layouts), item.isChecked());
                return true;
            case R.id.checkbox_detect_clicks:
                item.setChecked(!item.isChecked());
                setSharedPreference(packageName + getString(R.string.pref_detect_clicks), item.isChecked());
                return true;
            case R.id.item_delete_cache:
                FileHelper.deleteFile(packageName, "layoutsMap.bin");
                FileHelper.deleteFile(packageName, "reverseMap.bin");
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
        boolean checked = preferences.getBoolean(packageName + prefString, false);
        item.setChecked(checked);
    }
}
