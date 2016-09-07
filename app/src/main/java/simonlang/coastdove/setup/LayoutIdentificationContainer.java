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

package simonlang.coastdove.setup;

import java.util.*;

import simonlang.coastdove.detection.LayoutIdentification;
import simonlang.coastdove.utility.CollatorWrapper;
import simonlang.coastdove.utility.PowerSet;

/**
 * Contains a layout identification along with additional information
 * needed in order to build the layout map and the reverse map.
 */
public class LayoutIdentificationContainer {
    /** Object to contain necessary information for layout identification */
    private LayoutIdentification layoutIdentification;
    /** All "android:id" values the layout holds */
    private Set<String> androidIDs;
    /** The power set builder needed for the power set of android IDs */
    private PowerSet<String> powerSet;
//    /** The original XML document from which to parse */
//    private Document xmlDocument;
    /** Sets of android IDs best suited for identifying the layout */
    private List<Set<String>> bestIDSets;
    /** Ambiguity level, indicates how many layouts can be identified by the
     * same ID sets (if 1, the layout can be uniquely identified) */
    public int ambiguity;

    /**
     * Constructs a new LayoutIdentificationContainer from the given data
     * @param name           Name of the layout
     * @param androidIDs     "android:id" values for this layout
     */
    public LayoutIdentificationContainer(String name, Set<String> androidIDs) {
        this.ambiguity = 0;
        this.layoutIdentification = new LayoutIdentification(name);
        this.androidIDs = androidIDs;
        this.bestIDSets = new LinkedList<>();

        powerSet = new PowerSet<>(this.androidIDs, String[].class, new CollatorWrapper());
    }

    /**
     * Returns the layout's name
     * @return The layout's name
     */
    public String getName() {
        return layoutIdentification.getName();
    }

    /** The power set builder needed for the power set of android IDs */
    public PowerSet<String> getPowerSet() {
        return powerSet;
    }

    /** Sets of android IDs best suited for identifying the layout */
    public List<Set<String>> getBestIDSets() {
        return bestIDSets;
    }

    /** All "android:id" values the layout holds */
    public Set<String> getAndroidIDs() {
        return androidIDs;
    }

    /** Object to contain necessary information for layout identification */
    public LayoutIdentification getLayoutIdentification() {
        return layoutIdentification;
    }

    /**
     * Adds all bestIDSets as layout identifiers to the contained LayoutIdentification
     */
    public void addBestIDSetsAsLayoutIdentifiers() {
        getLayoutIdentification().addAllLayoutIdentifiers(getBestIDSets());
    }
}
