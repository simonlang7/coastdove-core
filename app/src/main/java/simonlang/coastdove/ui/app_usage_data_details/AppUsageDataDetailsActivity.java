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

package simonlang.coastdove.ui.app_usage_data_details;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import simonlang.coastdove.usage.AppUsageData;
import simonlang.coastdove.usage.sql.SQLiteDataRemover;
import simonlang.coastdove.utility.FileHelper;
import simonlang.coastdove.R;

/**
 * Activity that shows collected app usage data in detail
 */
public class AppUsageDataDetailsActivity extends AppCompatActivity {
    private String appPackageName;
    private String timestamp;
    private int appID;
    private AppUsageData appUsageData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_usage_data_details);

        retrieveDetails(null);

        // Set support action bar
        Toolbar toolbar = (Toolbar)findViewById(R.id.detectable_app_toolbar);
        toolbar.setTitle(this.appPackageName);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        retrieveDetails(null);
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
                String txtFilename = this.appUsageData.getTextFilename();
                if (!FileHelper.fileExists(this, FileHelper.Directory.PUBLIC_PACKAGE, this.appPackageName, txtFilename) &&
                        this.appUsageData != null) {
                    FileHelper.writeTxtFile(this, this.appUsageData.toStrings(), FileHelper.Directory.PUBLIC_PACKAGE, this.appPackageName, txtFilename);
                    Toast toast = Toast.makeText(this, getString(R.string.toast_saved_to_txt), Toast.LENGTH_SHORT);
                    toast.show();
                }
                else {
                    Toast toast = Toast.makeText(this, getString(R.string.toast_file_exists), Toast.LENGTH_SHORT);
                    toast.show();
                }
                return true;
            case R.id.item_delete:
                new SQLiteDataRemover(this, this.appID).run();
                Toast toast = Toast.makeText(this, getString(R.string.toast_data_removed), Toast.LENGTH_SHORT);
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
     * Returns the primary key of the app usage data
     */
    public int getAppID() {
        return this.appID;
    }

    /**
     * Sets the app usage data for this activity
     */
    public void setAppUsageData(AppUsageData appUsageData) {
        this.appUsageData = appUsageData;
    }

    /**
     * Retrieves details needed for this activity
     * @param savedInstanceState
     */
    private void retrieveDetails(Bundle savedInstanceState) {// Get details
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                this.appPackageName = null;
                this.timestamp = null;
                this.appID = -1;
            }
            else {
                this.appPackageName = extras.getString(getString(R.string.extras_package_name));
                this.timestamp = extras.getString(getString(R.string.extras_timestamp));
                this.appID = extras.getInt(getString(R.string.extras_app_id));
            }
        }
        else {
            this.appPackageName = (String)savedInstanceState.getSerializable(getString(R.string.extras_package_name));
            this.timestamp = (String)savedInstanceState.getSerializable(getString(R.string.extras_timestamp));
            this.appID = (int)savedInstanceState.getSerializable(getString(R.string.extras_app_id));
        }
    }
}
