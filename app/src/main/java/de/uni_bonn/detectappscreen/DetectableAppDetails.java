package de.uni_bonn.detectappscreen;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;

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
        setContentView(R.layout.activity_detectable_app_menu);

        // Get packageName
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null)
                this.packageName = null;
            else
                this.packageName = extras.getString("packageName"); // TODO: make constant
        }
        else
            this.packageName = (String)savedInstanceState.getSerializable("packageName");


        // Set support action bar
        Toolbar toolbar = (Toolbar)findViewById(R.id.detectable_app_toolbar);
        toolbar.setTitle(this.packageName);
        setSupportActionBar(toolbar);

        // todo: switches (how?)
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detectable_app_menu, menu);

        // Switch for enabling/disabling app detection
        final MenuItem detectAppSwitch = menu.findItem(R.id.detect_app);
        final SwitchCompat actionView = (SwitchCompat)detectAppSwitch.getActionView();
        actionView.setChecked(DetectAppScreenAccessibilityService.isDetectionDataLoadedOrLoading(packageName));
        actionView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    DetectAppScreenAccessibilityService.startLoadingDetectionData(packageName, false, true, true, getApplicationContext()); // todo: un-hardcode
                else
                    DetectAppScreenAccessibilityService.removeDetectionData(packageName);
            }
        });


        return super.onCreateOptionsMenu(menu);
    }
}
