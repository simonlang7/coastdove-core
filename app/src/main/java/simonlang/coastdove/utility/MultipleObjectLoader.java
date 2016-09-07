/*  Coast Dove
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

import android.app.Activity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import simonlang.coastdove.ui.LoadingInfo;

/**
 * Runs ObjectLoaders in a new Thread and keeps them in a map (Strings as keys) once finished.
 * Loading processes can be cancelled and the current status of each object can be retrieved.
 */
public class MultipleObjectLoader<T> {
    public enum Status {
        LOADED,
        LOADING,
        NONE
    }

    /** Map of Threads currently loading objects */
    private Map<String, Thread> loadingObjects;
    /** Map of LoadingInfos for loading objects */
    private Map<String, LoadingInfo> loadingInfos;
    /** Map of objects already loaded */
    private Map<String, T> loadedObjects;

    /**
     * Constructs a MultipleObjectLoader
     */
    public MultipleObjectLoader() {
        this.loadingObjects = new ConcurrentHashMap<>();
        this.loadingInfos = new ConcurrentHashMap<>();
        this.loadedObjects = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves the object for the given key, if it has finished loading
     * @param key    Key to identify the object
     * @return The object, if it has finished loading and the key exists; null otherwise
     */
    public T get(String key) {
        return loadedObjects.get(key);
    }

    /**
     * Interrupts the loading thread of the object for the given key
     * @param key    Key to identify the object
     */
    public void interrupt(String key) {
        synchronized (this) {
            if (loadingObjects.containsKey(key)) {
                loadingObjects.get(key).interrupt();
            }
        }
    }

    /**
     * Indicates whether the thread loading the object for the given key is currently interrupted
     * @param key    Key to identify the object
     * @return true if the thread is interrupted
     */
    public boolean isInterrupted(String key) {
        boolean result = false;
        synchronized (this) {
            if (loadingObjects.containsKey(key))
                result = loadingObjects.get(key).isInterrupted();
        }
        return result;
    }

    /**
     * Returns the loading status of an object
     * @param key    Key of the object to look for
     * @return LOADED if the object has finished loading, LOADING if it is loading,
     *         NONE if it does not exist
     */
    public Status getStatus(String key) {
        if (this.loadingObjects.containsKey(key))
            return Status.LOADING;
        if (this.loadedObjects.containsKey(key))
            return Status.LOADED;

        return Status.NONE;
    }

    /**
     * Returns all objects that have finished loading
     */
    public Collection<T> getAll() {
        return loadedObjects.values();
    }

    /**
     * Starts loading a new object
     * @param key       Key used to identify the object
     * @param loader    Loader to load the object with
     * @return True if loading was started, false if the key already exists
     */
    public boolean startLoading(String key, ObjectLoader<T> loader) {
        return startLoading(key, loader, null);
    }

    /**
     * Starts loading a new object
     * @param key            Key used to identify the object
     * @param loader         Loader to load the object with
     * @param loadingInfo    LoadingInfo for the object to load
     * @return True if loading was started, false if the key already exists
     */
    public boolean startLoading(String key, ObjectLoader<T> loader, LoadingInfo loadingInfo) {
        if (loadedObjects.containsKey(key) || loadingObjects.containsKey(key))
            return false;
        Thread thread = new Thread(loader);
        loadingObjects.put(key, thread);
        if (loadingInfo != null)
            loadingInfos.put(key, loadingInfo);
        thread.start();
        Log.d("MultipleObjectLoader", "Started loading " + key);
        return true;
    }

    /**
     * Removes the object and its loader thread
     * @param key    Key to identify the object
     * @return True if the object (or its loader thread) was removed, false otherwise
     */
    public boolean remove(String key) {
        boolean removed = false;
        synchronized (this) {
            if (loadingObjects.containsKey(key)) {
                    Thread thread = loadingObjects.get(key);
                    thread.interrupt();
                    loadingObjects.remove(key);
                Log.d("MultipleObjectLoader", "Removed loader thread for " + key);
                removed = true;
            }
        }
        if (loadedObjects.containsKey(key)) {
            loadedObjects.remove(key);
            Log.d("MultipleObjectLoader", "Removed loaded data for " + key);
            removed = true;
        }
        if (loadingInfos.containsKey(key))
            loadingInfos.remove(key);
        return removed;
    }

    /**
     * Checks whether there is an object that is being loaded or has finished loading
     * @param key    Key to identify the object
     * @return True if there is an object that is being loaded or has finished loading
     */
    public boolean contains(String key) {
        return loadedObjects.containsKey(key) || loadingObjects.containsKey(key);
    }

    /** Retrieves LoadingInfo for the given key */
    public LoadingInfo getLoadingInfo(String key) {
        return loadingInfos.get(key);
    }

    /**
     * Clears all UI elements of all loading infos - to be used when an activity is destroyed
     * @param origin    Origin of loading info
     */
    public void clearLoadingInfoUIElements(String origin) {
        for (LoadingInfo loadingInfo : loadingInfos.values()) {
            if (loadingInfo.getOrigin().equals(origin))
                loadingInfo.clearUIElements();
        }
    }

    /**
     * Updates the UI elements of all loading infos with the given origin
     * @param origin         Origin of loading infos
     * @param activity       Activity that contains the UI elements
     * @param listAdapter    ListAdapter to update
     */
    public void updateLoadingInfoUIElements(String origin, Activity activity, ArrayAdapter listAdapter) {
        for (LoadingInfo loadingInfo : loadingInfos.values()) {
            if (loadingInfo.getOrigin().equals(origin))
                loadingInfo.setUIElements(activity, listAdapter);
        }
    }

    /**
     * Updates the UI elements of all loading infos with the given origin
     * @param origin         Origin of loading infos
     * @param activity       Activity that contains the UI elements
     * @param progressBar    ProgressBar to update
     */
    public void updateLoadingInfoUIElements(String origin, Activity activity, ProgressBar progressBar) {
        for (LoadingInfo loadingInfo : loadingInfos.values()) {
            if (loadingInfo.getOrigin().equals(origin))
                loadingInfo.setUIElements(activity, progressBar);
        }
    }

    /**
     * Delivers the passed result to this MultipleObjectLoader, removing the loader thread
     * and adding the actual object to loadedObjects.
     * @param key       Key to identify the object
     * @param object    Object loaded
     */
    void deliverResult(String key, T object) {
        remove(key);
        loadedObjects.put(key, object);
        Log.d("MultipleObjectLoader", "Removed loader thread for " + key);
    }
}
