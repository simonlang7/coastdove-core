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

package de.uni_bonn.detectappscreen.ui.app_usage_data_details;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Iterator;
import java.util.List;

import de.uni_bonn.detectappscreen.analyze.AppUsageDataProcessor;
import de.uni_bonn.detectappscreen.analyze.MetaEntry;
import de.uni_bonn.detectappscreen.app_usage.AppUsageData;
import de.uni_bonn.detectappscreen.utility.FileHelper;
import de.uni_bonn.detectappscreen.R;

/**
 * Activity that shows collected app usage data in detail
 */
public class AppUsageDataDetailsActivity extends AppCompatActivity {
    private String appPackageName;
    private String filename;
    private AppUsageDataProcessor appUsageDataProcessor;

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
                String txtFilename = this.filename.replace(".json", ".txt");
                if (!FileHelper.fileExists(this, FileHelper.Directory.APP_USAGE_DATA_EXPORT, this.appPackageName, txtFilename) &&
                        this.appUsageDataProcessor != null) {
                    FileHelper.writeTxtFile(this, this.appUsageDataProcessor.toStrings(), FileHelper.Directory.APP_USAGE_DATA_EXPORT, this.appPackageName, txtFilename);
                    Toast toast = Toast.makeText(this, getString(R.string.toast_saved_to_txt), Toast.LENGTH_SHORT);
                    toast.show();
                }
                else {
                    Toast toast = Toast.makeText(this, getString(R.string.toast_file_exists), Toast.LENGTH_SHORT);
                    toast.show();
                }
                return true;
            case R.id.item_delete:
                FileHelper.deleteFile(this, FileHelper.Directory.APP_USAGE_DATA, this.appPackageName, this.filename);
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
    public void setAppUsageDataProcessor(AppUsageDataProcessor appUsageDataProcessor) {
        this.appUsageDataProcessor = appUsageDataProcessor;
    }
}
