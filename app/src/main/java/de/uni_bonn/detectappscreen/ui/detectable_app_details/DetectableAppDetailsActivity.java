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
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import de.uni_bonn.detectappscreen.detection.AppDetectionData;
import de.uni_bonn.detectappscreen.detection.AppDetectionDataLoader;
import de.uni_bonn.detectappscreen.detection.DetectAppScreenAccessibilityService;
import de.uni_bonn.detectappscreen.detection.ReplacementData;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.ui.add_app.AddAppActivity;
import de.uni_bonn.detectappscreen.utility.FileHelper;
import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.utility.Misc;
import de.uni_bonn.detectappscreen.utility.MultipleObjectLoader;

/**
 * Activity started when a detectable app in the main list is clicked,
 * displays a button to activate/deactivate detection, as well as a list
 * of detection sessions
 */
public class DetectableAppDetailsActivity extends AppCompatActivity {

    /** Origin for loading infos */
    public static final String ORIGIN = "DETECTABLE_APP_DETAILS";

    /** Package name of the app to be detected */
    private String appPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detectable_app_details);

        retrieveAppPackageName(savedInstanceState);

        // Set support action bar
        Toolbar toolbar = (Toolbar)findViewById(R.id.detectable_app_toolbar);
        toolbar.setTitle(this.appPackageName);
        setSupportActionBar(toolbar);

        final ProgressBar progressBar = (ProgressBar)findViewById(R.id.detectable_app_progress_bar);
        DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader()
                .updateLoadingInfoUIElements(ORIGIN, this, progressBar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader()
                .clearLoadingInfoUIElements(ORIGIN);
    }

    @Override
    protected void onResume() {
        super.onResume();

        retrieveAppPackageName(null);

        boolean detectionDataLoadedOrLoading = false;
        try {
            detectionDataLoadedOrLoading = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().contains(this.appPackageName);
        } catch (NullPointerException e) {
        }

        final ProgressBar progressBar = (ProgressBar)findViewById(R.id.detectable_app_progress_bar);
        final Context context = this;

        // Switch to activate detection of the specified app
        final Switch activateSwitch = (Switch)findViewById(R.id.detectable_app_activate_switch);
        activateSwitch.setChecked(detectionDataLoadedOrLoading);
        activateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Check whether to load layout and click detection
                    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
                    boolean detectLayouts = preferences.getBoolean(appPackageName + getString(R.string.pref_detect_layouts), Misc.DEFAULT_DETECT_LAYOUTS);
                    boolean detectInteractions = preferences.getBoolean(appPackageName + getString(R.string.pref_detect_interactions), Misc.DEFAULT_DETECT_INTERACTIONS);
                    boolean replacePrivateData = preferences.getBoolean(appPackageName + getString(R.string.pref_replace_private_data), Misc.DEFAULT_REPLACE_PRIVATE_DATA);
                    boolean replacementDataExists = FileHelper.fileExists(context, FileHelper.Directory.PUBLIC_PACKAGE, appPackageName, FileHelper.REPLACEMENT_DATA);

                    // Loading info UI elements
                    int uid = appPackageName.hashCode();
                    LoadingInfo loadingInfo = new LoadingInfo(getApplicationContext(), uid, ORIGIN);
                    loadingInfo.setUIElements(DetectableAppDetailsActivity.this, progressBar);

                    // Start the loading process and add
                    MultipleObjectLoader<AppDetectionData> multiLoader = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader();
                    AppDetectionDataLoader loader = new AppDetectionDataLoader(appPackageName, multiLoader,
                            detectLayouts, detectInteractions, replacePrivateData && replacementDataExists, context, loadingInfo);
                    multiLoader.startLoading(appPackageName, loader, loadingInfo);
                    Log.d("DetAppDetails", "Started loading with " + loadingInfo.isFinished() + " finished loadingInfo");
                }
                else
                    DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().remove(appPackageName);
                setUpASActivationBar();
            }
        });

        boolean cacheExists = FileHelper.appDetectionDataExists(context, appPackageName);
        setUpActivationBar(cacheExists);
        setUpASActivationBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detectable_app_details_menu, menu);

        // Set up the menu checkboxes
        AppDetectionData detectionData = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().get(this.appPackageName);
        if (detectionData != null) {
            boolean detectingLayouts = detectionData.getPerformLayoutChecks();
            boolean detectingInteractions = detectionData.getPerformInteractionChecks();
            SharedPreferences preferences = getPreferences(MODE_PRIVATE);
            if (Misc.getPreferenceBoolean(preferences, appPackageName, getString(R.string.pref_detect_layouts), Misc.DEFAULT_DETECT_LAYOUTS))
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_layouts), detectingLayouts);
            if (Misc.getPreferenceBoolean(preferences, appPackageName, getString(R.string.pref_detect_interactions), Misc.DEFAULT_DETECT_INTERACTIONS))
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_interactions), detectingInteractions);
        }

        setUpCheckboxes(menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem checkboxDetectLayouts = menu.findItem(R.id.checkbox_detect_layouts);
        MenuItem checkboxDetectClicks = menu.findItem(R.id.checkbox_detect_interactions);
        MenuItem itemDeleteCache = menu.findItem(R.id.item_delete_cache);
        try {
            boolean cacheExists = FileHelper.appDetectionDataExists(this, this.appPackageName);

            // If the detection data is currently in use, the menu items are disabled
            boolean detectionDataLoading = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().getStatus(this.appPackageName)
                    == MultipleObjectLoader.Status.LOADING;
            checkboxDetectLayouts.setEnabled(!detectionDataLoading);
            checkboxDetectClicks.setEnabled(!detectionDataLoading);
            itemDeleteCache.setEnabled(cacheExists && !detectionDataLoading);
        } catch (NullPointerException e) {
            checkboxDetectLayouts.setEnabled(false);
            checkboxDetectClicks.setEnabled(false);
            itemDeleteCache.setEnabled(false);
            Log.e("DetectableAppDetailsAc.", "Error creating menu: " + e.getMessage());
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Store options regarding layout / click detection
        final AppDetectionData detectionData = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().get(this.appPackageName);
        switch (item.getItemId()) {
            case R.id.checkbox_detect_layouts:
                item.setChecked(!item.isChecked());
                Misc.setPreference(getPreferences(MODE_PRIVATE), appPackageName, getString(R.string.pref_detect_layouts), item.isChecked());
                if (detectionData != null)
                    detectionData.setPerformLayoutChecks(item.isChecked());
                return true;
            case R.id.checkbox_detect_interactions:
                item.setChecked(!item.isChecked());
                Misc.setPreference(getPreferences(MODE_PRIVATE), appPackageName, getString(R.string.pref_detect_interactions), item.isChecked());
                if (detectionData != null)
                    detectionData.setPerformInteractionChecks(item.isChecked());
                return true;
            case R.id.checkbox_replace_private_data:
                item.setChecked(!item.isChecked());
                Misc.setPreference(getPreferences(MODE_PRIVATE), appPackageName, getString(R.string.pref_replace_private_data), item.isChecked());
                if (detectionData != null) {
                    if (item.isChecked()) {
                        ReplacementData replacementData = Misc.loadReplacementData(DetectableAppDetailsActivity.this, appPackageName);
                        detectionData.setReplacementData(replacementData);
                    }
                    else
                        detectionData.setReplacementData(null);
                }
                return true;
            case R.id.item_delete_cache:
                FileHelper.deleteFile(this, FileHelper.Directory.PRIVATE_PACKAGE, this.appPackageName, FileHelper.APP_DETECTION_DATA_FILENAME);
                final TextView activateText = (TextView)findViewById(R.id.detectable_app_activate_text);
                final Switch activateSwitch = (Switch)findViewById(R.id.detectable_app_activate_switch);
                activateSwitch.setChecked(false);
                setUpActivationBar(false);
                activateText.invalidate();
                activateSwitch.invalidate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void retrieveAppPackageName(Bundle savedInstanceState) {
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
    }

    /**
     * Returns the package name of the app to be detected
     */
    public String getAppPackageName() {
        return this.appPackageName;
    }

    /**
     * Sets up this view's menu checkboxes according to the saved preferences
     */
    private void setUpCheckboxes(Menu menu) {
        MenuItem checkboxLayouts = menu.findItem(R.id.checkbox_detect_layouts);
        MenuItem checkboxInteractions = menu.findItem(R.id.checkbox_detect_interactions);
        MenuItem checkboxReplacePrivateData = menu.findItem(R.id.checkbox_replace_private_data);
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean detectLayouts = Misc.getPreferenceBoolean(preferences, this.appPackageName, getString(R.string.pref_detect_layouts), Misc.DEFAULT_DETECT_LAYOUTS);
        boolean detectInteractions = Misc.getPreferenceBoolean(preferences, this.appPackageName,
                getString(R.string.pref_detect_interactions), Misc.DEFAULT_DETECT_INTERACTIONS);
        boolean replacePrivateData = Misc.getPreferenceBoolean(preferences, this.appPackageName,
                getString(R.string.pref_replace_private_data), Misc.DEFAULT_REPLACE_PRIVATE_DATA);
        checkboxLayouts.setChecked(detectLayouts);
        checkboxInteractions.setChecked(detectInteractions);

        boolean detectionDataInUse = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().contains(this.appPackageName);
        boolean replacementDataExists = FileHelper.fileExists(this, FileHelper.Directory.PUBLIC_PACKAGE, this.appPackageName, FileHelper.REPLACEMENT_DATA);
        checkboxReplacePrivateData.setEnabled(replacementDataExists && !detectionDataInUse);
        checkboxReplacePrivateData.setChecked(replacementDataExists && replacePrivateData);
    }

    /**
     * Sets up the "Activate" text and switch
     * @param cacheExists    Whether the serialized AppDetectionData exists or not
     */
    private void setUpActivationBar(boolean cacheExists) {
        final TextView activateText = (TextView)findViewById(R.id.detectable_app_activate_text);
        final Switch activateSwitch = (Switch)findViewById(R.id.detectable_app_activate_switch);
        final FrameLayout activationBar = (FrameLayout)findViewById(R.id.detectable_app_activation_bar);

        activateSwitch.setEnabled(cacheExists);
        activateSwitch.setVisibility(cacheExists ? View.VISIBLE : View.INVISIBLE);
        if (cacheExists) {
            activateText.setText(getString(R.string.activate));

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activateSwitch.setChecked(!activateSwitch.isChecked());
                }
            };

            activateText.setOnClickListener(listener);
            activationBar.setOnClickListener(listener);
        }
        else {
            activateText.setText(getString(R.string.activate_no_cache));

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(DetectableAppDetailsActivity.this, AddAppActivity.class));
                }
            };

            activateText.setOnClickListener(listener);
            activationBar.setOnClickListener(listener);
        }
    }

    /**
     * Sets up a bar to remind the user to activate the accessibility service
     */
    private void setUpASActivationBar() {
        final LinearLayout activateASBar = (LinearLayout)findViewById(R.id.detectable_app_accessibility_activation_bar);
        boolean detectionDataLoadedOrLoading = false;
        try {
            detectionDataLoadedOrLoading = DetectAppScreenAccessibilityService.getAppDetectionDataMultiLoader().contains(this.appPackageName);
        } catch (NullPointerException e) {
        }

        boolean asActive = Misc.isAccessibilityServiceActive(this);

        if (detectionDataLoadedOrLoading && !asActive)
            activateASBar.setVisibility(View.VISIBLE);
        else
            activateASBar.setVisibility(View.GONE);
    }

    /**
     * Opens the accessibility services menu
     * @param view    View that triggered the function
     */
    public void openAccessibilityServicesMenu(View view) {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }
}
