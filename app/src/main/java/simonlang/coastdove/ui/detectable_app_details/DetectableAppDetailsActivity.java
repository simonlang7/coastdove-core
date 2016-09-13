/*  Coast Dove
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

package simonlang.coastdove.ui.detectable_app_details;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

import simonlang.coastdove.detection.AppDetectionData;
import simonlang.coastdove.detection.AppDetectionDataLoader;
import simonlang.coastdove.detection.CoastDoveService;
import simonlang.coastdove.detection.ReplacementData;
import simonlang.coastdove.ui.LoadingInfo;
import simonlang.coastdove.ui.add_app.AddAppActivity;
import simonlang.coastdove.utility.FileHelper;
import simonlang.coastdove.R;
import simonlang.coastdove.utility.Misc;
import simonlang.coastdove.utility.MultipleObjectLoader;

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
        CoastDoveService.appDetectionDataMultiLoader
                .updateLoadingInfoUIElements(ORIGIN, this, progressBar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CoastDoveService.appDetectionDataMultiLoader
                .clearLoadingInfoUIElements(ORIGIN);
    }

    @Override
    protected void onResume() {
        super.onResume();

        retrieveAppPackageName(null);

        boolean detectionDataLoadedOrLoading = false;
        try {
            detectionDataLoadedOrLoading = CoastDoveService.appDetectionDataMultiLoader.contains(this.appPackageName);
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
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    boolean detectLayouts = preferences.getBoolean(appPackageName + getString(R.string.pref_detect_layouts), Misc.DEFAULT_DETECT_LAYOUTS);
                    boolean detectInteractions = preferences.getBoolean(appPackageName + getString(R.string.pref_detect_interactions), Misc.DEFAULT_DETECT_INTERACTIONS);
                    boolean detectScreenState = preferences.getBoolean(appPackageName + getString(R.string.pref_detect_screen_state), Misc.DEFAULT_DETECT_SCREEN_STATE);
                    boolean detectNotifications = preferences.getBoolean(appPackageName + getString(R.string.pref_detect_notifications), Misc.DEFAULT_DETECT_NOTIFICATIONS);
                    boolean replacePrivateData = preferences.getBoolean(appPackageName + getString(R.string.pref_replace_private_data), Misc.DEFAULT_REPLACE_PRIVATE_DATA);
                    boolean replacementDataExists = FileHelper.fileExists(context, FileHelper.Directory.PUBLIC_PACKAGE, appPackageName, FileHelper.REPLACEMENT_DATA);

                    // Loading info UI elements
                    int uid = appPackageName.hashCode();
                    LoadingInfo loadingInfo = new LoadingInfo(getApplicationContext(), uid, ORIGIN);
                    loadingInfo.setUIElements(DetectableAppDetailsActivity.this, progressBar);

                    // Start the loading process and add
                    MultipleObjectLoader<AppDetectionData> multiLoader = CoastDoveService.appDetectionDataMultiLoader;
                    AppDetectionDataLoader loader = new AppDetectionDataLoader(appPackageName, multiLoader,
                            detectLayouts, detectInteractions, detectScreenState, detectNotifications, replacePrivateData && replacementDataExists,
                            context, loadingInfo);
                    multiLoader.startLoading(appPackageName, loader, loadingInfo);
                    Log.d("DetAppDetails", "Started loading with " + loadingInfo.isFinished() + " finished loadingInfo");
                }
                else
                    CoastDoveService.appDetectionDataMultiLoader.remove(appPackageName);
                setUpReminderBars();
            }
        });

        boolean cacheExists = FileHelper.appDetectionDataExists(context, appPackageName);
        setUpActivationBar(cacheExists);
        setUpReminderBars();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detectable_app_details_menu, menu);

        // Set up the menu checkboxes
        AppDetectionData detectionData = CoastDoveService.appDetectionDataMultiLoader.get(this.appPackageName);
        if (detectionData != null) {
            boolean detectingLayouts = detectionData.getPerformLayoutChecks();
            boolean detectingInteractions = detectionData.getPerformInteractionChecks();
            boolean detectingScreenState = detectionData.getPerformScreenStateChecks();
            boolean detectingNotifications = detectionData.getPerformNotificationChecks();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (Misc.getPreferenceBoolean(preferences, appPackageName, getString(R.string.pref_detect_layouts), Misc.DEFAULT_DETECT_LAYOUTS))
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_layouts), detectingLayouts);
            if (Misc.getPreferenceBoolean(preferences, appPackageName, getString(R.string.pref_detect_interactions), Misc.DEFAULT_DETECT_INTERACTIONS))
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_interactions), detectingInteractions);
            if (Misc.getPreferenceBoolean(preferences, appPackageName, getString(R.string.pref_detect_screen_state), Misc.DEFAULT_DETECT_SCREEN_STATE))
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_screen_state), detectingScreenState);
            if (Misc.getPreferenceBoolean(preferences, appPackageName, getString(R.string.pref_detect_notifications), Misc.DEFAULT_DETECT_NOTIFICATIONS))
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_notifications), detectingNotifications);
        }

        setUpCheckboxes(menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem checkboxDetectLayouts = menu.findItem(R.id.checkbox_detect_layouts);
        MenuItem checkboxDetectInteractions = menu.findItem(R.id.checkbox_detect_interactions);
        MenuItem checkboxDetectScreenState = menu.findItem(R.id.checkbox_detect_screen_state);
        MenuItem checkboxDetectNotifications = menu.findItem(R.id.checkbox_detect_notifications);
        MenuItem checkboxReplacePrivateData = menu.findItem(R.id.checkbox_replace_private_data);
        MenuItem itemDeleteReplacementMap = menu.findItem(R.id.item_delete_replacement_mapping);
        MenuItem itemDeleteCache = menu.findItem(R.id.item_delete_cache);
        try {
            boolean cacheExists = FileHelper.appDetectionDataExists(this, this.appPackageName);
            boolean replacementDataExists = FileHelper.fileExists(this, FileHelper.Directory.PUBLIC_PACKAGE, this.appPackageName, FileHelper.REPLACEMENT_DATA);
            boolean replacementMappingExists = FileHelper.fileExists(this, FileHelper.Directory.PRIVATE_PACKAGE, this.appPackageName, FileHelper.REPLACEMENT_MAP);

            // If the detection data is currently in use, the menu items are disabled
            boolean detectionDataLoading = CoastDoveService.appDetectionDataMultiLoader.getStatus(this.appPackageName)
                    == MultipleObjectLoader.Status.LOADING;
            checkboxDetectLayouts.setEnabled(!detectionDataLoading);
            checkboxDetectInteractions.setEnabled(!detectionDataLoading);
            checkboxDetectScreenState.setEnabled(!detectionDataLoading);
            checkboxDetectNotifications.setEnabled(!detectionDataLoading);
            checkboxReplacePrivateData.setEnabled(!detectionDataLoading && replacementDataExists);
            itemDeleteReplacementMap.setEnabled(!detectionDataLoading && replacementMappingExists);
            itemDeleteCache.setEnabled(cacheExists && !detectionDataLoading);
        } catch (NullPointerException e) {
            checkboxDetectLayouts.setEnabled(false);
            checkboxDetectInteractions.setEnabled(false);
            checkboxDetectScreenState.setEnabled(false);
            checkboxDetectNotifications.setEnabled(false);
            itemDeleteReplacementMap.setEnabled(false);
            itemDeleteCache.setEnabled(false);
            Log.e("DetectableAppDetailsAc.", "Error creating menu: " + e.getMessage());
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Store options regarding layout / click detection
        final AppDetectionData detectionData = CoastDoveService.appDetectionDataMultiLoader.get(this.appPackageName);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        switch (item.getItemId()) {
            case R.id.checkbox_detect_layouts:
                item.setChecked(!item.isChecked());
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_layouts), item.isChecked());
                if (detectionData != null)
                    detectionData.setPerformLayoutChecks(item.isChecked());
                return true;
            case R.id.checkbox_detect_interactions:
                item.setChecked(!item.isChecked());
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_interactions), item.isChecked());
                if (detectionData != null)
                    detectionData.setPerformInteractionChecks(item.isChecked());
                return true;
            case R.id.checkbox_detect_screen_state:
                item.setChecked(!item.isChecked());
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_screen_state), item.isChecked());
                if (detectionData != null)
                    detectionData.setPerformScreenStateChecks(item.isChecked());
                return true;
            case R.id.checkbox_detect_notifications:
                item.setChecked(!item.isChecked());
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_detect_notifications), item.isChecked());
                if (detectionData != null)
                    detectionData.setPerformNotificationChecks(item.isChecked());
                return true;
            case R.id.checkbox_replace_private_data:
                item.setChecked(!item.isChecked());
                Misc.setPreference(preferences, appPackageName, getString(R.string.pref_replace_private_data), item.isChecked());
                if (detectionData != null) {
                    if (item.isChecked()) {
                        ReplacementData replacementData = Misc.loadReplacementData(DetectableAppDetailsActivity.this, appPackageName);
                        detectionData.setReplacementData(replacementData);
                    }
                    else
                        detectionData.setReplacementData(null);
                }
                return true;
            case R.id.item_delete_replacement_mapping:
                FileHelper.deleteFile(this, FileHelper.Directory.PRIVATE_PACKAGE, this.appPackageName, FileHelper.REPLACEMENT_MAP);
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
        MenuItem checkboxScreenState = menu.findItem(R.id.checkbox_detect_screen_state);
        MenuItem checkboxNotifications = menu.findItem(R.id.checkbox_detect_notifications);
        MenuItem checkboxReplacePrivateData = menu.findItem(R.id.checkbox_replace_private_data);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean detectLayouts = Misc.getPreferenceBoolean(preferences, this.appPackageName,
                getString(R.string.pref_detect_layouts), Misc.DEFAULT_DETECT_LAYOUTS);
        boolean detectInteractions = Misc.getPreferenceBoolean(preferences, this.appPackageName,
                getString(R.string.pref_detect_interactions), Misc.DEFAULT_DETECT_INTERACTIONS);
        boolean detectScreenState = Misc.getPreferenceBoolean(preferences, this.appPackageName,
                getString(R.string.pref_detect_screen_state), Misc.DEFAULT_DETECT_SCREEN_STATE);
        boolean detectNotifications = Misc.getPreferenceBoolean(preferences, this.appPackageName,
                getString(R.string.pref_detect_notifications), Misc.DEFAULT_DETECT_NOTIFICATIONS);
        boolean replacePrivateData = Misc.getPreferenceBoolean(preferences, this.appPackageName,
                getString(R.string.pref_replace_private_data), Misc.DEFAULT_REPLACE_PRIVATE_DATA);

        checkboxLayouts.setChecked(detectLayouts);
        checkboxInteractions.setChecked(detectInteractions);
        checkboxScreenState.setChecked(detectScreenState);
        checkboxNotifications.setChecked(detectNotifications);

        boolean replacementDataExists = FileHelper.fileExists(this, FileHelper.Directory.PUBLIC_PACKAGE, this.appPackageName, FileHelper.REPLACEMENT_DATA);
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
    private void setUpReminderBars() {
        final LinearLayout activateASBar = (LinearLayout)findViewById(R.id.detectable_app_accessibility_activation_bar);
        final LinearLayout accuracyWarningBar = (LinearLayout)findViewById(R.id.detectable_app_accuracy_warning_bar);
        boolean detectionDataLoadedOrLoading = false;

        try {
            detectionDataLoadedOrLoading = CoastDoveService.appDetectionDataMultiLoader.contains(this.appPackageName);
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
