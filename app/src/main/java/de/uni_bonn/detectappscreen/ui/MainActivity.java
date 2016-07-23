package de.uni_bonn.detectappscreen.ui;


import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import de.uni_bonn.detectappscreen.R;


/**
 * Main activity that is started when the app starts
 */
public class MainActivity extends AppCompatActivity {

    private static final String APP_NAME = "DetectAppScreen";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Opens the system's Accessibility Services menu that can otherwise be accessed via Android Settings
     */
    public void openAccessibilityServices(View view) {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

}