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
    private String packageName;

    /** Name of the sub-directory to use */
    private String subDirectory;

    /** Files to be excluded from the list */
    private List<String> filesToExclude;

    /**
     * Creates a new FileListLoader using the given data
     * @param context           Application context
     * @param appName           Name of the app, needed for the external storage public directory
     * @param filesToExclude    Files to be excluded from the list
     */
    public FileListLoader(Context context, String appName, String subDirectory, String... filesToExclude) {
        super(context);
        this.packageName = appName;
        this.filesToExclude = new LinkedList<>();
        for (String file : filesToExclude)
            this.filesToExclude.add(file);
        this.subDirectory = subDirectory != null ? subDirectory : "";
    }

    /**
     * Creates a new FileListLoader using the given data
     * @param context           Application context
     * @param packageName           Name of the app, needed for the external storage public directory
     */
    public FileListLoader(Context context, String packageName, String subDirectory) {
        super(context);
        this.packageName = packageName;
        this.filesToExclude = new LinkedList<>();
        this.subDirectory = subDirectory != null ? subDirectory : "";
    }

    /**
     * Loads the list of files in the external storage public directory of the given app name, usually
     * /sdcard/{packageName}/{subDirectory}/, and delivers the result
     */
    @Override
    public void onStartLoading() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(packageName), subDirectory);
        String[] files = directory.exists() ? directory.list() : new String[0];
        ArrayList<String> data = new ArrayList<>(files.length);
        for (int i = 0; i < files.length; ++i) {
            if (!this.filesToExclude.contains(files[i]))
                data.add(files[i]);
        }

        deliverResult(data);
    }
}