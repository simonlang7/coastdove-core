package de.uni_bonn.detectappscreen;

import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class DetectableAppMenu extends AppCompatActivity {

    protected String appName;

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

        final ListView actionsListView = (ListView)findViewById(R.id.actionsList);
        List<String> actions = new ArrayList<>(3);
        actions.add("Activate");
        actions.add("View");

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, actions);
        actionsListView.setAdapter(adapter);
        final String appToStart = this.appName;

        actionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final String item = (String)parent.getItemAtPosition(position);
                switch (item) {
                    case "Activate":
                        DetectAppScreenAccessibilityService.startLoadingDetectionData(appToStart);
                    case "View":
                    default:
                }
            }
        });
    }

    public void buildHashmaps(View view) {
        DetectAppScreenAccessibilityService.startLoadingDetectionData(this.appName);
    }
}
