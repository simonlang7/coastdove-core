package de.uni_bonn.detectappscreen;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

public class DetectableAppDetails extends AppCompatActivity {

    private String appName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detectable_app_menu);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null)
                this.appName = null;
            else
                this.appName = extras.getString("appName"); // TODO: make constant
        }
        else
            this.appName = (String)savedInstanceState.getSerializable("appName");


        Toolbar toolbar = (Toolbar)findViewById(R.id.detectable_app_toolbar);
        toolbar.setTitle(this.appName);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detectable_app_menu, menu);

        final MenuItem detectAppSwitch = menu.findItem(R.id.detect_app);
        final SwitchCompat actionView = (SwitchCompat)detectAppSwitch.getActionView();
        actionView.setChecked(DetectAppScreenAccessibilityService.isDetectionDataLoadedOrLoading(appName));
        actionView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    DetectAppScreenAccessibilityService.startLoadingDetectionData(appName);
                else
                    DetectAppScreenAccessibilityService.removeDetectionData(appName);
            }
        });
        return super.onCreateOptionsMenu(menu);
    }
}
