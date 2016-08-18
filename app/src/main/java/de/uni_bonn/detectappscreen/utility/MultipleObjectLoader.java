package de.uni_bonn.detectappscreen.utility;

import android.util.Log;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs ObjectLoaders in a new Thread and keeps them in a map (Strings as keys) once finished.
 * Loading processes can be cancelled and the current status of each object can be retrieved.
 */
public class MultipleObjectLoader<T> {
    /** Map of Threads currently loading objects */
    private Map<String, Thread> loadingObjects;
    /** Map of objects already loaded */
    private Map<String, T> loadedObjects;

    /**
     * Constructs a MultipleObjectLoader
     */
    public MultipleObjectLoader() {
        this.loadingObjects = new ConcurrentHashMap<>();
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
        if (loadedObjects.containsKey(key) || loadingObjects.containsKey(key))
            return false;
        Thread thread = new Thread(loader);
        loadingObjects.put(key, thread);
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
        if (loadingObjects.containsKey(key)) {
            Thread thread = loadingObjects.get(key);
            thread.interrupt();
            loadingObjects.remove(key);
            Log.d("MultipleObjectLoader", "Removed loader thread for " + key);
            removed = true;
        }
        if (loadedObjects.containsKey(key)) {
            loadedObjects.remove(key);
            Log.d("MultipleObjectLoader", "Removed loaded data for " + key);
            removed = true;
        }
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

    /**
     * Delivers the passed result to this MultipleObjectLoader, removing the loader thread
     * and adding the actual object to loadedObjects.
     * @param key       Key to identify the object
     * @param object    Object loaded
     */
    void deliverResult(String key, T object) {
        loadedObjects.put(key, object);
        loadingObjects.remove(key);
        Log.d("MultipleObjectLoader", "Removed loader thread for " + key);
    }
}
