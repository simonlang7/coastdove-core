package de.uni_bonn.detectappscreen.ui;

import android.content.Context;
import android.support.v4.content.Loader;
import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
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

    /** Extension to filter */
    private String extension;

    /**
     * Creates a new FileListLoader using the given data
     * @param context           Application context
     * @param appName           Name of the app, needed for the external storage public directory
     * @param filesToExclude    Files to be excluded from the list
     * @param extension             Extension to filter for, e.g. ".json" or ".txt"
     */
    public FileListLoader(Context context, String appName, String subDirectory, String extension, String... filesToExclude) {
        super(context);
        this.packageName = appName;
        this.filesToExclude = new LinkedList<>();
        for (String file : filesToExclude)
            this.filesToExclude.add(file);
        this.subDirectory = subDirectory != null ? subDirectory : "";
        this.extension = extension == null ? "" : extension;
    }

    /**
     * Creates a new FileListLoader using the given data
     * @param context               Application context
     * @param packageName           Name of the app, needed for the external storage public directory
     * @param extension             Extension to filter for, e.g. ".json" or ".txt"
     */
    public FileListLoader(Context context, String packageName, String subDirectory, String extension) {
        super(context);
        this.packageName = packageName;
        this.filesToExclude = new LinkedList<>();
        this.subDirectory = subDirectory != null ? subDirectory : "";
        this.extension = extension == null ? "" : extension;
    }

    /**
     * Loads the list of files in the external storage public directory of the given app name, usually
     * /sdcard/{packageName}/{subDirectory}/, and delivers the result
     */
    @Override
    public void onStartLoading() {
        File directory = new File(Environment.getExternalStoragePublicDirectory(packageName), subDirectory);
        String[] files = directory.exists() ? directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(extension);
            }
        }) : new String[0];
        ArrayList<String> data = new ArrayList<>(files.length);
        for (int i = 0; i < files.length; ++i) {
            if (!this.filesToExclude.contains(files[i]))
                data.add(files[i]);
        }

        deliverResult(data);
    }
}