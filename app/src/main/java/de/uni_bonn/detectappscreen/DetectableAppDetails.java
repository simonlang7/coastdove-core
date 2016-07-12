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
public class DetectableAppDetails extends AppCompatActivity {

    /** Name of the app to be detected */
    private String packageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detectable_app_details);

        // Get packageName
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

        final Switch activateSwitch = (Switch)findViewById(R.id.detectable_app_activate_switch);
        activateSwitch.setChecked(DetectAppScreenAccessibilityService.isDetectionDataLoadedOrLoading(this.packageName));
        activateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
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

        // Set up the three menu checkboxes
        setUpCheckbox(menu.findItem(R.id.checkbox_detect_layouts), getString(R.string.pref_detect_layouts));
        setUpCheckbox(menu.findItem(R.id.checkbox_detect_clicks), getString(R.string.pref_detect_clicks));

        // Switch for enabling/disabling app detection
//        final MenuItem detectAppSwitch = menu.findItem(R.id.detect_app);
//        final SwitchCompat actionView = (SwitchCompat)detectAppSwitch.getActionView();
//        actionView.setChecked(DetectAppScreenAccessibilityService.isDetectionDataLoadedOrLoading(packageName));
//        actionView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked)
//                    DetectAppScreenAccessibilityService.startLoadingDetectionData(packageName, false, true, true, getApplicationContext()); // todo: un-hardcode
//                else
//                    DetectAppScreenAccessibilityService.removeDetectionData(packageName);
//            }
//        });


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.checkbox_detect_layouts:
                item.setChecked(!item.isChecked());
                setSharedPreference(packageName + getString(R.string.pref_detect_layouts), item.isChecked());
                return false;
            case R.id.checkbox_detect_clicks:
                item.setChecked(!item.isChecked());
                setSharedPreference(packageName + getString(R.string.pref_detect_clicks), item.isChecked());
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void activateDetectableApp(View view) {
        Switch activateSwitch = (Switch)findViewById(R.id.detectable_app_activate_switch);
        activateSwitch.setChecked(!activateSwitch.isChecked());
    }

    private void setSharedPreference(String preference, boolean value) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(preference, value);
        editor.commit();
    }

    private void setUpCheckbox(MenuItem item, String prefString) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean checked = preferences.getBoolean(packageName + prefString, false);
        item.setChecked(checked);
    }
}
