package de.uni_bonn.detectappscreen.utility;

import android.support.annotation.NonNull;

/**
 * Loader for any type of objects, meant to be started using a Thread.
 * Takes care of executing deliverResult methods in the given MultipleObjectLoader
 * Override the load() method to implement your loading mechanism.
 */
public abstract class ObjectLoader<T> implements Runnable {

    /** Key by which the object loader (and optionally the object) is identified */
    protected String key;
    /** Loader that takes care of running this ObjectLoader */
    protected MultipleObjectLoader<T> multipleObjectLoader;

    /**
     * Constructs an ObjectLoader with the given key and the MultipleObjectLoader that initialized it
     * @param key                     Key by which the loader is identified
     * @param multipleObjectLoader    Loader that initialized this ObjectLoader
     */
    public ObjectLoader(@NonNull String key, @NonNull MultipleObjectLoader<T> multipleObjectLoader) {
        this.key = key;
        this.multipleObjectLoader = multipleObjectLoader;
    }

    /**
     * Executes load() on this object, then delivers the result using deliverResult() on the
     * MultipleObjectLoader
     */
    @Override
    public final void run() {
        T object = this.load();
        this.multipleObjectLoader.deliverResult(key, object);
    }

    /**
     * Loads the object to be loaded
     * @return The loaded object
     */
    protected abstract T load();
}
