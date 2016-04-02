package de.uni_bonn.detectappscreen;

import android.content.Intent;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class InfoActivity extends AppCompatActivity {

    protected static List<Pair<String, JSONObject>> screenDefinitions = null;

    public static List<Pair<String, JSONObject>> getScreenDefinitions() {
        return new LinkedList<>(screenDefinitions);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));

        // TODO: temporary solution: read one json file for comparison
        screenDefinitions = new LinkedList<>();
        File[] testJsons = new File[2];
        testJsons[0] = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), "home.json");
        testJsons[1] = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), "conversation.json");

        for (File testJson : testJsons) {
            try (InputStream is = new FileInputStream(testJson);
                 InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader br = new BufferedReader(isr)) {
                String line = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
                InfoActivity.screenDefinitions.add(new Pair<>(testJson.getName(), new JSONObject(stringBuilder.toString())));
            } catch (FileNotFoundException e) {
                Log.e("WDebug", "File not found: " + testJson.getAbsolutePath());
                Log.e("WDebug", e.getMessage());
            } catch (JSONException e) {
                Log.e("WDebug", "Unable to create JSONObject from file (" + testJson.getAbsolutePath() + "): " + e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
