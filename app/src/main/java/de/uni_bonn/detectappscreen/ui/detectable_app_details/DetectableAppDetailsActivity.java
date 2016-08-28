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

package de.uni_bonn.detectappscreen.ui.detectable_app_details;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;

import de.uni_bonn.detectappscreen.detection.AppDetectionData;
import de.uni_bonn.detectappscreen.detection.AppDetectionDataLoader;
import de.uni_bonn.detectappscreen.detection.DetectAppScreenAccessibilityService;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.utility.FileHelper;
import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.utility.MultipleObjectLoader;

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

        final ProgressBar progressBar = (ProgressBar)findViewById(R.id.detectable_app_progress_bar);
        final Context context = this;

        // Switch to activate detection of the specified app
        boolean detectionDataLoadedOrLoading = false;

        try {
            detectionDataLoadedOrLoading = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().contains(this.appPackageName);
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

                    // Loading info UI elements
                    int uid = appPackageName.hashCode();
                    LoadingInfo loadingInfo = new LoadingInfo(DetectableAppDetailsActivity.this,
                            uid, progressBar, true);

                    // Start the loading process and add
                    MultipleObjectLoader<AppDetectionData> multiLoader = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader();
                    AppDetectionDataLoader loader = new AppDetectionDataLoader(appPackageName, multiLoader,
                            detectLayouts, detectClicks, context, loadingInfo);
                    multiLoader.startLoading(appPackageName, loader);
                }
                else
                    DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().remove(appPackageName);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detectable_app_details_menu, menu);

        // Set up the menu checkboxes
        setUpCheckbox(menu.findItem(R.id.checkbox_detect_layouts), getString(R.string.pref_detect_layouts));
        setUpCheckbox(menu.findItem(R.id.checkbox_detect_interactions), getString(R.string.pref_detect_clicks));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem checkboxDetectLayouts = menu.findItem(R.id.checkbox_detect_layouts);
        MenuItem checkboxDetectClicks = menu.findItem(R.id.checkbox_detect_interactions);
        MenuItem itemDeleteCache = menu.findItem(R.id.item_delete_cache);
        try {
            boolean cacheExists = FileHelper.fileExists(this, FileHelper.Directory.PACKAGE, getAppPackageName(), "layouts.bin")
                    && FileHelper.fileExists(this, FileHelper.Directory.PACKAGE, getAppPackageName(), "reverseMap.bin");

            // If the detection data is currently in use, the menu items are disabled
            boolean detectionDataInUse = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().contains(this.appPackageName);
            checkboxDetectLayouts.setEnabled(!detectionDataInUse);
            checkboxDetectClicks.setEnabled(!detectionDataInUse);
            itemDeleteCache.setEnabled(cacheExists && !detectionDataInUse);
        } catch (NullPointerException e) {
            checkboxDetectLayouts.setEnabled(false);
            checkboxDetectClicks.setEnabled(false);
            itemDeleteCache.setEnabled(false);
            Log.i("DetectableAppDetailsAc.", "Error creating menu: " + e.getMessage());
        }

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
            case R.id.checkbox_detect_interactions:
                item.setChecked(!item.isChecked());
                setSharedPreference(appPackageName + getString(R.string.pref_detect_clicks), item.isChecked());
                return true;
            case R.id.item_delete_cache:
                FileHelper.deleteFile(this, FileHelper.Directory.PACKAGE, this.appPackageName, "layouts.bin");
                FileHelper.deleteFile(this, FileHelper.Directory.PACKAGE, this.appPackageName, "reverseMap.bin");
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
