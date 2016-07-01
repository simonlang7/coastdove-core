package de.uni_bonn.detectappscreen;


import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


/**
 * Created by Slang on 21.06.2016.
 */
public class MainActivity extends AppCompatActivity {

    private static final String APP_NAME = "DetectAppScreen";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view);

        Toolbar toolbar = (Toolbar)findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
    }

    public void openAccessibilityServices(View view) {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

}