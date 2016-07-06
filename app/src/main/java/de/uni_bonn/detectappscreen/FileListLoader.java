package de.uni_bonn.detectappscreen;

import android.content.Context;
import android.support.v4.content.Loader;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Loader for a list of files in a directory
 */
public class FileListLoader extends Loader<ArrayList<String>> {

    /** Name of the app, needed for the external storage public directory */
    private String appName;

    /** Files to be excluded from the list */
    private List<String> filesToExclude;

    /**
     * Creates a new FileListLoader using the given data
     * @param context           Application context
     * @param appName           Name of the app, needed for the external storage public directory
     * @param filesToExclude    Files to be excluded from the list
     */
    public FileListLoader(Context context, String appName, List<String> filesToExclude) {
        super(context);
        this.appName = appName;
        this.filesToExclude = filesToExclude;
    }

    /**
     * Creates a new FileListLoader using the given data
     * @param context           Application context
     * @param appName           Name of the app, needed for the external storage public directory
     */
    public FileListLoader(Context context, String appName) {
        super(context);
        this.appName = appName;
        this.filesToExclude = new LinkedList<>();
    }

    /**
     * Loads the list of files in the external storage public directory of the given app name, usually
     * /sdcard/{appName}/, and delivers the result
     */
    @Override
    public void onStartLoading() {
        File directory = Environment.getExternalStoragePublicDirectory(appName);
        String[] files = directory.list();
        ArrayList<String> data = new ArrayList<>(files.length);
        for (int i = 0; i < files.length; ++i) {
            if (!this.filesToExclude.contains(files[i]))
                data.add(files[i]);
        }

        deliverResult(data);
    }
}