package de.uni_bonn.detectappscreen;

import android.content.Context;
import android.support.v4.content.Loader;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Slang on 24.06.2016.
 */

public class FileListLoader extends Loader<ArrayList<String>> {

    private String appName;

    private List<String> filesToExclude;

    public FileListLoader(Context context, String appName, List<String> filesToExclude) {
        super(context);
        this.appName = appName;
        this.filesToExclude = filesToExclude;
    }

    public FileListLoader(Context context, String appName) {
        super(context);
        this.appName = appName;
        this.filesToExclude = new LinkedList<>();
    }

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

    public void setFilesToExclude(List<String> filesToExclude) {
        this.filesToExclude = filesToExclude;
    }
}