package de.uni_bonn.detectappscreen;

import android.os.Environment;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Created by Slang on 10.03.2016.
 */
public class AEventReader {
    protected static JSONObject readAEventFromFile(String filename) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Environment.getExternalStoragePublicDirectory("AEvents"), filename);
            JSONObject result;

            String jsonString = null;

            try(Scanner scanner = new Scanner(file)) {
                jsonString = scanner.useDelimiter("\\Z").next();
            } catch (FileNotFoundException e) {
                Log.e("DetectAppScreen", "Unable to read file " + file.getAbsolutePath());
                Log.e("DetectAppScreen", e.getMessage());
                return null;
            }

            try {
                result = new JSONObject(jsonString);
            } catch (JSONException e) {
                Log.e("DetectAppScreen", "Unable to create JSON Object from file (" + file.getName() + "): " + e.getMessage());
                return null;
            }

            return result;
        }
        else
            return null;
    }
}
