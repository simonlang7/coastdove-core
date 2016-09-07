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

package simonlang.coastdove.detection;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

import simonlang.coastdove.utility.CollatorWrapper;
import simonlang.coastdove.utility.SetSizeComparator;

/**
 * Contains data needed in order to identify a certain layout of an app.
 * Usually, each of the layouts in res/layout/ can be identified using the according LayoutIdentification.
 */
public class LayoutIdentification implements Serializable {
    private static final long serialVersionUID = 3285957059773072822L;

    /** Name of the layout to be identified, e.g. anything in res/layout/ of the according app */
    protected String name;
    /** Ambiguity factor; shows the number of ambiguous layouts to be identified the same way as this one.
     *  If 1, this layout is uniquely identified. */
    protected int ambiguity;
    /** Sets of android IDs used to identify this layout. Any one set is enough to identify this layout. */
    protected Set<Set<String>> layoutIdentifiers;

    public LayoutIdentification(String name) {
        this.name = name;
        this.layoutIdentifiers = new TreeSet<>(new SetSizeComparator());
    }

    /**
     * Creates a new layout identification using the given data
     * @param name                 Name of the layouts to be identified
     * @param ambiguity            Number of layouts that are identified by exactly the same android IDs
     * @param layoutIdentifiers    Sets of android IDs, each of which can identify the according layout
     */
    public LayoutIdentification(String name, int ambiguity, Set<Set<String>> layoutIdentifiers) {
        this.name = name;
        this.ambiguity = ambiguity;
        this.layoutIdentifiers = layoutIdentifiers;
    }

    /**
     * Creates a new layout identification using the given data
     * @param name                 Name of the layouts to be identified
     * @param ambiguity            Number of layouts that are identified by exactly the same android IDs
     * @param layoutIdentifiers    Sets of android IDs, each of which can identify the according layout
     */
    public LayoutIdentification(String name, int ambiguity, JSONArray layoutIdentifiers) {
        this.name = name;
        this.ambiguity = ambiguity;
        this.layoutIdentifiers = new CopyOnWriteArraySet<>();
        try {
            for (int i = 0; i < layoutIdentifiers.length(); ++i) {
                Set<String> currentSet = new TreeSet<>(new CollatorWrapper());
                JSONArray currentArray = layoutIdentifiers.getJSONArray(i);
                for (int j = 0; j < currentArray.length(); ++j)
                    currentSet.add(currentArray.getString(j));
                this.layoutIdentifiers.add(currentSet);
            }
        } catch (JSONException e) {
            Log.e("LayoutIdentification", "Error reading JSONObject for LayoutIdentification " + name + ": " + e.getMessage());
        }
    }

    /**
     * Sets of android IDs used to identify this layout. Any one set is enough to identify this layout.
     */
    public Set<Set<String>> getLayoutIdentifiers() {
        return layoutIdentifiers;
    }

    /**
     * Ambiguity factor; shows the number of ambiguous layouts to be identified the same way as this one.
     * If 1, this layout is uniquely identified.
     * */
    public int getAmbiguity() {
        return ambiguity;
    }

    /**
     * Adds a set of androidIDs as an identifier for this layout
     * @param identifier    set of "android:id" values to identify this layout
     */
    public void addLayoutIdentifier(Set<String> identifier) {
        this.layoutIdentifiers.add(identifier);
    }

    /**
     * Adds all sets of androidIDs in the given collection as identifiers for this layout
     * @param identifiers    collections of sets of "android:id" values, each of which
     *                       can identify this layout
     */
    public void addAllLayoutIdentifiers(Collection<Set<String>> identifiers) {
        this.layoutIdentifiers.addAll(identifiers);
    }

    /**
     * Name of the layout to be identified, e.g. anything in res/layout/ of the according app
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAmbiguity(int ambiguity) {
        this.ambiguity = ambiguity;
    }

    public void setLayoutIdentifiers(Set<Set<String>> layoutIdentifiers) {
        this.layoutIdentifiers = layoutIdentifiers;
    }

    /**
     * Converts this object to a JSONObject
     * @return a JSONObject with this LayoutIdentification's contents
     */
    public JSONObject toJSON() {
        JSONObject result = new JSONObject();

        try {
            result.put("_type", "LayoutIdentification");
            result.put("name", getName());
            JSONArray layoutIdentifiersAsJSON = new JSONArray();
            for (Set<String> identifier : layoutIdentifiers) {
                JSONArray identifierAsJSON = new JSONArray(identifier);
                layoutIdentifiersAsJSON.put(identifierAsJSON);
            }
            result.put("layoutIdentifiers", layoutIdentifiersAsJSON);
        } catch (JSONException e) {
            System.err.println("Error saving LayoutIdentification (" +
                    getName() + ") to JSON: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }
}
