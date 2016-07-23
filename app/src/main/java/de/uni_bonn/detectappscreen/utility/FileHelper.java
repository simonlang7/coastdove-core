package de.uni_bonn.detectappscreen.utility;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StreamCorruptedException;
import java.util.HashMap;

import de.uni_bonn.detectappscreen.R;

/**
 * Functions to help with reading from and writing to files
 */
public class FileHelper {
    /**
     * Reads a JSON file from the external storage public directory with the given sub-directory and filename
     * @param subDirectory    Sub-directory to use
     * @param filename        Filename of the file to read
     * @return A JSONObject with the file's contents
     */
    public static JSONObject readJSONFile(String subDirectory, String filename) {
        JSONObject result = null;

        File file = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), subDirectory + "/" + filename);
        try (InputStream is = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            result = new JSONObject(stringBuilder.toString());
        } catch (FileNotFoundException e) {
            Log.e("FileHelper", "File not found: " + file.getAbsolutePath());
            Log.e("FileHelper", e.getMessage());
        } catch (JSONException e) {
            Log.e("FileHelper", "Unable to create JSONObject from file (" + file.getAbsolutePath() + "): " + e.getMessage());
        } catch (IOException e) {
            Log.e("FileHelper", "Unable to create JSONObject from file (" + file.getAbsolutePath() + "): " + e.getMessage());
        }

        return result;
    }

    /**
     * Writes a String array to a text file
     * @param lines           Array to write
     * @param subDirectory    Sub-directory to create the file in
     * @param filename        Filename to write to
     */
    public static void writeTxtFile(Context context, String[] lines, String subDirectory, String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), subDirectory + "/" + filename);
        File directory = file.getParentFile();
        directory.mkdirs();
        try (OutputStream os = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(os);
             BufferedWriter br = new BufferedWriter(osw)) {
            for (int i = 0; i < lines.length; ++i)
                br.write(lines[i] + "\n");
        } catch (FileNotFoundException e) {
            Log.e("FileHelper", "File not found: " + file.getAbsolutePath());
            Log.e("FileHelper", e.getMessage());
        } catch (IOException e) {
            Log.e("FileHelper", "Unable to write to file (" + file.getAbsolutePath() + "): " + e.getMessage());
        }
        scanFile(context, file.getAbsolutePath());
    }

    /**
     * Lets the media scanner scan any files given
     * @param paths Absolute paths of the files to scan
     */
    public static void scanFile(Context context, String... paths) {
        MediaScannerConnection.scanFile(context, paths, null, new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {

            }

            @Override
            public void onScanCompleted(String path, Uri uri) {
                Log.v("Media scanner", "Scanned file: " + path);
            }
        });
    }

    public static void writeHashMap(Context context, HashMap hashMap, String subDirectory, String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory(context.getString(R.string.external_folder_name)), subDirectory + "/" + filename);
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(hashMap);
        } catch (FileNotFoundException e) {
            Log.e("FileHelper", "File not found: " + file.getAbsolutePath());
            Log.e("FileHelper", e.getMessage());
        } catch (IOException e) {
            Log.e("FileHelper", "Input/Output error (" + file.getAbsoluteFile() + "): " + e.getMessage());
        }
        scanFile(context, file.getAbsolutePath());
    }

    public static HashMap readHashMap(String subDirectory, String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), subDirectory + "/" + filename);
        HashMap result = null;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            result = (HashMap)ois.readObject();
        } catch (FileNotFoundException e) {
            Log.e("FileHelper", "File not found: " + file.getAbsolutePath());
            Log.e("FileHelper", e.getMessage());
        } catch (StreamCorruptedException e) {
            Log.e("FileHelper", "Stream corrupted: " + file.getAbsolutePath());
            Log.e("FileHelper", e.getMessage());
        } catch (IOException e) {
            Log.e("FileHelper", "Input/Output error (" + file.getAbsoluteFile() + "): " + e.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("FileHelper", "Class not found (" + file.getAbsoluteFile() + "): " + e.getMessage());
        }

        return result;
    }

    public static boolean deleteFile(String subDirectory, String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), subDirectory + "/" + filename);
        return file.delete();
    }

    public static boolean fileExists(String subDirectory, String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory("DetectAppScreen"), subDirectory + "/" + filename);
        return file.exists();
    }
}
