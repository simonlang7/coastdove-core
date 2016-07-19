package de.uni_bonn.detectappscreen;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

/**
 * Activity that shows collected app usage data in detail
 */
public class AppUsageDataDetailsActivity extends AppCompatActivity {
    private String appPackageName;
    private String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_usage_data_details);

        // Get package name
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                this.appPackageName = null;
                this.filename = null;
            }
            else {
                this.appPackageName = extras.getString(getString(R.string.extras_package_name));
                this.filename = extras.getString(getString(R.string.extras_filename));
            }
        }
        else {
            this.appPackageName = (String) savedInstanceState.getSerializable(getString(R.string.extras_package_name));
            this.filename = (String) savedInstanceState.getSerializable(getString(R.string.extras_filename));
        }

        // Set support action bar
        Toolbar toolbar = (Toolbar)findViewById(R.id.detectable_app_toolbar);
        toolbar.setTitle(this.filename);
        setSupportActionBar(toolbar);
    }

    /**
     * Returns the package name of the app to be detected
     */
    public String getAppPackageName() {
        return this.appPackageName;
    }

    /**
     * Returns the filename of the app usage data
     */
    public String getFilename() {
        return this.filename;
    }
}
