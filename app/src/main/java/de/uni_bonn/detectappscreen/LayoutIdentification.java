package de.uni_bonn.detectappscreen;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.Collator;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Slang on 29.05.2016.
 */
public class LayoutIdentification {
    protected String name;
    protected int ambiguity;
    protected Set<Set<String>> layoutIdentifiers;

    public LayoutIdentification(String name, int ambiguity, Set<Set<String>> layoutIdentifiers) {
        this.name = name;
        this.ambiguity = ambiguity;
        this.layoutIdentifiers = layoutIdentifiers;
    }

    public LayoutIdentification(String name, int ambiguity, JSONArray layoutIdentifiers) {
        this.name = name;
        this.ambiguity = ambiguity;
        this.layoutIdentifiers = new CopyOnWriteArraySet<>();
        try {
            for (int i = 0; i < layoutIdentifiers.length(); ++i) {
                Set<String> currentSet = new TreeSet<>(Collator.getInstance());
                JSONArray currentArray = layoutIdentifiers.getJSONArray(i);
                for (int j = 0; j < currentArray.length(); ++j)
                    currentSet.add(currentArray.getString(j));
                this.layoutIdentifiers.add(currentSet);
            }
        } catch (JSONException e) {
            Log.e("WDebug", "Error reading LayoutIdentification " + name + ": " + e.getMessage());
        }
    }

    public Set<Set<String>> getLayoutIdentifiers() {
        return layoutIdentifiers;
    }

    public int getAmbiguity() {
        return ambiguity;
    }

    public String getName() {
        return name;
    }
}
