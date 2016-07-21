package de.uni_bonn.detectappscreen;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * Activity that shows collected app usage data in detail
 */
public class AppUsageDataDetailsActivity extends AppCompatActivity {
    private String appPackageName;
    private String filename;
    private AppUsageData appUsageData;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_usage_data_details_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Store options regarding layout / click detection
        switch (item.getItemId()) {
            case R.id.item_export_to_txt:
                if (!FileHelper.fileExists(appPackageName + "/" + getString(R.string.app_usage_data_folder_name), this.filename) &&
                        this.appUsageData != null) {
                    FileHelper.writeTxtFile(this.appUsageData.toStrings(), appPackageName + "/" + getString(R.string.app_usage_data_folder_name),
                            this.filename.replace(".json", ".txt"));
                    Toast toast = Toast.makeText(this, getString(R.string.toast_saved_to_txt), Toast.LENGTH_SHORT);
                    toast.show();
                }
                else {
                    Toast toast = Toast.makeText(this, getString(R.string.toast_file_exists), Toast.LENGTH_SHORT);
                    toast.show();
                }
                return true;
            case R.id.item_delete:
                FileHelper.deleteFile(appPackageName + "/" + getString(R.string.app_usage_data_folder_name), this.filename);
                Toast toast = Toast.makeText(this, getString(R.string.toast_file_deleted), Toast.LENGTH_SHORT);
                toast.show();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    /**
     * Sets the app usage data for this activity
     */
    public void setAppUsageData(AppUsageData appUsageData) {
        this.appUsageData = appUsageData;
    }
}
