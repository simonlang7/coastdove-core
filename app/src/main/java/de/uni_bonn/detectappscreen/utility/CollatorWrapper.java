package de.uni_bonn.detectappscreen.utility;

import java.io.IOException;
import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;

/**
 * Source: http://stackoverflow.com/questions/8346036/serializing-collator-instance
 * Wrapper for Collator which isn't serializable. Thanks, Java.
 */
public class CollatorWrapper implements Comparator<String>, Serializable {
    private static final long serialVersionUID = 355565414282682127L;

    private transient Collator collatorInstance;

    public CollatorWrapper() {
        super();
        initCollatorInstance();
    }

    @Override
    public int compare(final String o1, final String o2) {
        return collatorInstance.compare(o1, o2);
    }

    private void initCollatorInstance() {
        collatorInstance = Collator.getInstance();
    }

    private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initCollatorInstance();
    }
}