package de.uni_bonn.detectappscreen;

import android.content.Intent;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InfoActivity extends AppCompatActivity {

    protected static JSONObject jsonObject = null;

    public static JSONObject getJSONObject() {
        return jsonObject;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));


        File testJson = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), "main_activity.json");
        StringBuilder stringBuilder = new StringBuilder();

        try (InputStream is = new FileInputStream(testJson);
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            String line = "";
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            InfoActivity.jsonObject = new JSONObject(stringBuilder.toString());
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
