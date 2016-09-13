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

package simonlang.coastdove.utility;

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

import simonlang.coastdove.R;
import simonlang.coastdove.usage.sql.AppUsageDbHelper;
import simonlang.coastdove.detection.AppDetectionData;

/**
 * A collection of functions to help with reading from and writing to files
 */
public class FileHelper {
    public static final String APP_DETECTION_DATA_FILENAME = "AppDetectionData.bin";
    public static final String EXPORTED_DB_FILENAME = "Exported.sqlite";
    public static final String REPLACEMENT_DATA = "ReplacementData.json";
    public static final String REPLACEMENT_MAP = "ReplacementMap.bin";

    /**
     * Type of directory, to be used when referring to a file
     */
    public enum Directory {
        /** The app's private directory */
        PRIVATE,
        /** The external storage public directory of this app */
        PUBLIC,
        /** The app's package name as a sub-directory of the app's private directory */
        PRIVATE_PACKAGE,
        /** The app's package name as a sub-directory of the app's public directory */
        PUBLIC_PACKAGE
    }


    /**
     * Reads a JSON file from the external storage public directory with the given directory and filename
     * @param context         App context
     * @param directory       Directory to use - if PUBLIC, appPackageName is ignored and can be null
     * @param appPackageName  Package name of the app to whose directory the JSON file belongs
     * @param filename        Filename of the file to read
     * @return A JSONObject with the file's contents
     */
    public static JSONObject readJSONFile(Context context, Directory directory, String appPackageName, String filename) {
        JSONObject result = null;

        File file = getFile(context, directory, appPackageName, filename);
        if (file == null || !file.exists())
            return null;

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
        } catch (JSONException | IOException e) {
            Log.e("FileHelper", "Unable to create JSONObject from file (" + file.getAbsolutePath() + "): " + e.getMessage());
        }

        return result;
    }

    /**
     * Writes a String array to a text file
     * @param context         App context
     * @param lines           Lines to write to the text file
     * @param directory       Directory to use - if PUBLIC, appPackageName is ignored and can be null
     * @param appPackageName  Package name of the app to whose directory the JSON file belongs
     * @param filename        Filename to write to
     */
    public static void writeTxtFile(Context context, String[] lines, Directory directory, String appPackageName, String filename) {
        File file = getFile(context, directory, appPackageName, filename);
        if (file == null)
            return;

        makeParentDir(file);

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

    public static void writeAppDetectionData(Context context, AppDetectionData data, Directory directory, String appPackageName, String filename) {
        File file = getFile(context, directory, appPackageName, filename);
        if (file == null)
            throw new RuntimeException("Cannot write to " + file.getAbsolutePath());

        makeParentDir(file);

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(data);
        } catch (FileNotFoundException e) {
            Log.e("FileHelper", "File not found: " + file.getAbsolutePath());
            Log.e("FileHelper", e.getMessage());
        } catch (IOException e) {
            Log.e("FileHelper", "Input/Output error (" + file.getAbsoluteFile() + "): " + e.getMessage());
        }
        scanFile(context, file.getAbsolutePath());
    }

    public static AppDetectionData readAppDetectionData(Context context, Directory directory, String appPackageName, String filename) {
        File file = getFile(context, directory, appPackageName, filename);
        if (file == null)
            throw new RuntimeException("Cannot read from " + file.getAbsolutePath());

        AppDetectionData result = null;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            result = (AppDetectionData)ois.readObject();
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

    public static void writeHashMap(Context context, HashMap hashMap, Directory directory, String appPackageName, String filename) {
        File file = getFile(context, directory, appPackageName, filename);
        if (file == null)
            return;

        makeParentDir(file);

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

    public static HashMap readHashMap(Context context, Directory directory, String appPackageName, String filename) {
        File file = getFile(context, directory, appPackageName, filename);
        if (file == null)
            return null;

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

    public static void exportSQLiteDB(Context context, Directory directory, String filename) {
        AppUsageDbHelper dbHelper = new AppUsageDbHelper(context);
        File internalDBFile = new File(dbHelper.getReadableDatabase().getPath());
        File exportDBFile = getFile(context, directory, null, filename);
        makeParentDir(exportDBFile);

        // Copy binary file
        try (FileInputStream fis = new FileInputStream(internalDBFile);
             FileOutputStream fos = new FileOutputStream(exportDBFile)) {
            byte[] buffer = new byte[1024];
            int numBytes = 0;
            while ((numBytes = fis.read(buffer)) != -1)
                fos.write(buffer, 0, numBytes);
            FileHelper.scanFile(context, exportDBFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            Log.e("FileHelper", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e("FileHelper", "IO error: " + e.getMessage());
        } finally {
            dbHelper.close();
        }
    }

    public static boolean deleteFile(Context context, Directory directory, String appPackageName, String filename) {
        File file = getFile(context, directory, appPackageName, filename);
        return file != null && file.delete();
    }

    public static boolean fileExists(Context context, Directory directory, String appPackageName, String filename) {
        File file = getFile(context, directory, appPackageName, filename);
        return file != null && file.exists();
    }

    public static boolean appDetectionDataExists(Context context, String appPackageName) {
        File detectionData = FileHelper.getFile(context, FileHelper.Directory.PRIVATE_PACKAGE,
                appPackageName, FileHelper.APP_DETECTION_DATA_FILENAME);
        return detectionData != null && detectionData.exists();
    }

    public static File getFile(Context context, Directory directory, String appPackageName, String filename) {
        String publicDirectory = context.getString(R.string.external_folder_name);
        File baseDirectory;
        String subDirectory;
        switch (directory) {
            case PRIVATE:
                baseDirectory = context.getFilesDir();
                subDirectory = "";
                break;
            case PUBLIC:
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Log.e("FileHelper", "External storage is not mounted, failing.");
                    return null;
                }
                baseDirectory = Environment.getExternalStoragePublicDirectory(publicDirectory);
                subDirectory = "";
                break;
            case PRIVATE_PACKAGE:
                baseDirectory = context.getFilesDir();
                if (appPackageName == null)
                    throw new IllegalArgumentException("appPackageName must not be null");
                subDirectory = appPackageName + "/";
                break;
            case PUBLIC_PACKAGE:
                if (appPackageName == null)
                    throw new IllegalArgumentException("appPackageName must not be null");
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Log.e("FileHelper", "External storage is not mounted, failing.");
                    return null;
                }
                baseDirectory = Environment.getExternalStoragePublicDirectory(publicDirectory);
                subDirectory = appPackageName + "/";
                break;
            default:
                throw new IllegalArgumentException("directory must be specified (see FileHelper.Directory)");
        }

        File file = new File(baseDirectory, subDirectory + filename);
        return file;
    }

    private static void makeParentDir(File file) {
        File path = file.getParentFile();
        path.mkdirs();
    }
}
