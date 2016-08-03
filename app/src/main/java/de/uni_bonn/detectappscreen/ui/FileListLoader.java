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

package de.uni_bonn.detectappscreen.ui;

import android.content.Context;
import android.support.v4.content.Loader;
import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.uni_bonn.detectappscreen.utility.CollatorWrapper;

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

        Collections.sort(data, new CollatorWrapper());
        deliverResult(data);
    }
}