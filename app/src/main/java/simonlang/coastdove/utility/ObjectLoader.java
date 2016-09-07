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
        if (Thread.currentThread().isInterrupted()) {
            this.multipleObjectLoader.remove(key);
            return;
        }

        this.multipleObjectLoader.deliverResult(key, object);
    }

    /**
     * Loads the object to be loaded
     * @return The loaded object
     */
    protected abstract T load();
}
