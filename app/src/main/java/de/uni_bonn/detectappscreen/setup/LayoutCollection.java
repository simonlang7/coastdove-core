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

package de.uni_bonn.detectappscreen.setup;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import brut.androlib.res.decoder.AXmlResourceParser;
import de.uni_bonn.detectappscreen.R;
import de.uni_bonn.detectappscreen.detection.LayoutIdentification;
import de.uni_bonn.detectappscreen.ui.LoadingInfo;
import de.uni_bonn.detectappscreen.utility.MultipleObjectLoader;
import de.uni_bonn.detectappscreen.utility.PowerSet;
import de.uni_bonn.detectappscreen.utility.XMLHelper;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.Collator;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Collection of LayoutIdentification objects, intended for all layouts of one app, needed
 * to determine unique identifiers for layouts
 */
public class LayoutCollection {
    private static final String JSON_TYPE = "LayoutCollection";
    private static final int JSON_VERSION_MAJOR = 0;
    private static final int JSON_VERSION_MINOR = 4;

    /** Context for UI stuff */
    private Context context;

    /** MultipleObjectLoader for layout collections, used in AddAppActivity */
    private static MultipleObjectLoader<LayoutCollection> layoutCollectionMultipleObjectLoader = new MultipleObjectLoader<>();

    /** MultipleObjectLoader for layout collections, used in AddAppActivity, todo: find better place? */
    public static MultipleObjectLoader<LayoutCollection> getLayoutCollectionMultipleObjectLoader() {
        return layoutCollectionMultipleObjectLoader;
    }

    /** Each layout identification object represents one of the app's layouts */
    private List<LayoutIdentification> layoutIdentificationList;
    /** Set of all "android:id" attributes that occur throughout all layouts */
    private Set<String> allAndroidIDs;
    /** Map that maps from android IDs to possibly recognizable layouts */
    private ReverseMap reverseMap;

    /**
     * Constructs a new layout collection from the given list
     * @param layoutIdentificationList    Layouts to process
     */
    public LayoutCollection(List<LayoutIdentification> layoutIdentificationList) {
        this.layoutIdentificationList = layoutIdentificationList;
    }

    /**
     * Constructs a new layout collection from the given .apk file
     * @param apk                 APK file to process
     * @param minDetectionRate    minimal target detection rate (i.e. (#detectable layouts)/(#layouts)),
     *                            value between 0f and 1f
     */
    public LayoutCollection(Context context, ZipFile apk, String appPackageName, float minDetectionRate, LoadingInfo loadingInfo) {
        Log.d("LayoutCollection", "Constructor");
        this.layoutIdentificationList = new LinkedList<>();
        this.allAndroidIDs = new TreeSet<>(Collator.getInstance());
        this.context = context;

        loadingInfo.setNotificationData(context.getString(R.string.add_app_notification_loading),
                appPackageName, R.drawable.notification_template_icon_bg);
        loadingInfo.start(true);

        Log.d("LayoutCollection", "Parsing resources.arsc");
        ARSCFileParser resourceParser = new ARSCFileParser();
        ZipEntry arscEntry = apk.getEntry("resources.arsc");
        try {
            resourceParser.parse(apk.getInputStream(arscEntry));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("LayoutCollection", "Reading zip file");
        Enumeration<?> zipEntries = apk.entries();
        List<LayoutIdentificationContainer> containers = new LinkedList<>();
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry)zipEntries.nextElement();
            try {
                String entryName = zipEntry.getName();
                if (entryName.contains("res/layout/") && XMLHelper.isBinaryXML(apk.getInputStream(zipEntry))) {
                    String name = entryName.replaceAll(".*/", "").replace(".xml", "");

                    AXmlResourceParser parser = new AXmlResourceParser();
                    parser.open(apk.getInputStream(zipEntry));
                    Set<String> androidIDs = parseAndroidIDs(parser, resourceParser);

                    LayoutIdentificationContainer container = new LayoutIdentificationContainer(name, androidIDs);
                    this.layoutIdentificationList.add(container.getLayoutIdentification());
                    containers.add(container);

                    this.allAndroidIDs.addAll(container.getAndroidIDs());
                }
            } catch (IOException e) {
                Log.e("LayoutCollection", "Error reading APK file: " + e.getMessage());
            }
        }

        // get unique IDs and reverse map
        List<LayoutIdentificationContainer> containersCopy = new LinkedList<>(containers);
        lookupUniqueIDs(containersCopy);
        this.reverseMap = new ReverseMap(containers, this.allAndroidIDs);

        loadingInfo.setNotificationData(context.getString(R.string.add_app_notification_finished_loading),
                null, null);
        loadingInfo.end();
    }

    /**
     * Converts this object to JSON
     * @return JSONObject containing the layout identifications of this collection
     */
    public JSONObject toJSON() {
        JSONObject result = new JSONObject();
        try {
            result.put("_type", JSON_TYPE);
            result.put("_versionMajor", JSON_VERSION_MAJOR);
            result.put("_versionMinor", JSON_VERSION_MINOR);
            JSONArray layoutDefinitionsAsJSON = new JSONArray();
            for (LayoutIdentification layoutIdentification : layoutIdentificationList)
                layoutDefinitionsAsJSON.put(layoutIdentification.toJSON());
            result.put("layoutDefinitions", layoutDefinitionsAsJSON);

        } catch (JSONException e) {
            System.err.println("Error converting LayoutCollection to JSON: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        return result;
    }

    /** Map that maps from android IDs to possibly recognizable layouts */
    public ReverseMap getReverseMap() {
        return this.reverseMap;
    }

    /** Each layout identification object represents one of the app's layouts */
    public List<LayoutIdentification> getLayoutIdentificationList() {
        return this.layoutIdentificationList;
    }

    /**
     * Parses all "android:id" values using the given XmlResourceParser. The parser must be opened before
     * calling this function.
     * @param parser            Parser to parse the data from
     * @param resourceParser    ARSC parser to parse resource strings from, in case the XML is parsed from binary.
     *                          If null, resource strings are not replaced.
     * @return Set of all "android:id" values occurring in the XML file
     */
    private Set<String> parseAndroidIDs(XmlPullParser parser, @Nullable ARSCFileParser resourceParser) {
        Set<String> result = new TreeSet<>(Collator.getInstance());
        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG)
                    continue;
                for (int i = 0; i < parser.getAttributeCount(); ++i) {
                    if (parser.getAttributeName(i).equals("id")) {
                        String androidID = parser.getAttributeValue(i).substring(1);
                        // Parse resource string if needed
                        if (resourceParser != null) {
                            int resourceID = Integer.parseInt(androidID);
                            ARSCFileParser.AbstractResource resource = resourceParser.findResource(resourceID);
                            if (resource == null)
                                continue; // cannot be parsed, do not add
                            androidID = "id/" + resource.getResourceName();
                        }
                        result.add(androidID);
                    }
                }
            }
        } catch (XmlPullParserException e) {
            Log.e("LayoutCollection", "Unable to parse from XML file: " + e.getMessage());
        } catch (IOException e) {
            Log.e("LayoutCollection", "IO error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Given a set of layouts (containers), this function looks at all "android:id" attributes in
     * each of them, and finds sets of androidIDs that are best suited for recognizing the layout,
     * i.e. sets that uniquely identify a layout. The final data is saved in each LayoutIdentification
     * which is held in its own container
     * @param containers    Layouts (containers) to process
     */
    private void lookupUniqueIDs(List<LayoutIdentificationContainer> containers) {
        int layoutsCount = containers.size();

        // The fewer androidIDs we need to identify a layout, the better, hence start with size 1
        for (int currentSize = 1; currentSize < 16 && containers.size() > 0; ++currentSize) {
            Log.d("LayoutCollection", "Size: " + currentSize);
            Map<String, Integer> idOccurrenceCounts = idOccurrenceCounts(containers, currentSize);

            int count = 1;
            int currentMax = containers.size();
            // What's the current "best" for identifiers?
            Iterator<LayoutIdentificationContainer> it = containers.iterator();
            while (it.hasNext()) {
                if (currentSize >= 5)
                    Log.d("LayoutCollection", count + " / " + currentMax);

                LayoutIdentificationContainer container = it.next();

                updateBestIDSets(container, currentSize, idOccurrenceCounts);
                boolean removeContainer = checkRemoveContainer(container, currentSize);
                if (removeContainer)
                    it.remove();

                ++count;
            }

            //System.out.println("\nidOccurrenceCounts size: " + idOccurrenceCounts.size());
            Log.d("LayoutCollection", "Accuracy: " + (layoutsCount - containers.size())/(float)layoutsCount);
        }

        // Check the leftovers
        for (LayoutIdentificationContainer container : containers) {
            if (container.ambiguity == 1) {
                container.addBestIDSetsAsLayoutIdentifiers();
            }
        }
    }

    /**
     * Returns a map that, for each set of IDs (ordered and converted to a single String), stores how often that set occurs
     * among all layouts.
     *
     * Example:
     * {layout1}: {id1, id2, id3}
     * {layout2}: {id2, id3}
     * {layout3}: {id1, id3, id4}
     *
     * idOccurrenceCounts: {([id1], 2), ([id2], 2), ([id3], 3), ([id4], 1), // size 1
     *                      ([id1, id2], 1), ([id1, id3]: 2), ([id1, id4]: 1), ([id2, id3]: 2), ([id3, id4]: 1),  // size 2, [id2, id4] does not occur
     *                      ... }
     *
     * @param containers     Layouts (i.e. their identification data) to process
     * @param currentSize    Size of subsets to consider (i.e. every subset with #currentSize IDs)
     * @return A map with sets of IDs (combined into Strings) as keys, and the number of times they occur together (in layouts) as values
     */
    private HashMap<String, Integer> idOccurrenceCounts(List<LayoutIdentificationContainer> containers, int currentSize) {
        HashMap<String, Integer> result = new HashMap<>(this.allAndroidIDs.size() * 50); // only approximate size
        // How often does each set of androidIDs occur in total?
        for (LayoutIdentificationContainer container : containers) {
            PowerSet<String> powerSet = container.getPowerSet();
            powerSet.startOver(currentSize);
            while (powerSet.hasNext() && powerSet.sizeOfNext() == currentSize) {
                Collection<String> subset = powerSet.next();
                String androidIDs = subset.toString();

                if (!result.containsKey(androidIDs))
                    result.put(androidIDs, 0);

                int newIdOccurrenceCount = result.get(androidIDs) + 1;
                result.put(androidIDs, newIdOccurrenceCount);
            }
        }

        return result;
    }

    /**
     * Updates the given layout (container) in that it finds sets of android IDs (bestIDSets)
     * that are best-suited for layout recognition, i.e. have the lowest ambiguity and (preferably)
     * a smaller size
     * @param container             Container of the layout for which to update the best ID sets
     * @param currentSize           Size of subsets to consider (i.e. every subset with #currentSize IDs)
     * @param idOccurrenceCounts    The ID occurrence counts map, as retrieved
     *                              from {@link LayoutCollection#idOccurrenceCounts(List, int)}
     */
    private void updateBestIDSets(LayoutIdentificationContainer container, int currentSize,
                                  Map<String, Integer> idOccurrenceCounts) {
        PowerSet<String> powerSet = container.getPowerSet();
        powerSet.startOver(currentSize);

        while (powerSet.hasNext() && powerSet.sizeOfNext() == currentSize) {
            Set<String> subset = powerSet.next();
            String androidIDs = subset.toString();

            int idOccurrenceCount = idOccurrenceCounts.get(androidIDs);
            if (container.getBestIDSets().isEmpty() // first?
                    || idOccurrenceCount <= container.ambiguity) { // occurs less often?
                if (container.getBestIDSets().isEmpty() || idOccurrenceCount < container.ambiguity) {
                    // If the ambiguity is less, clear everything we have collected so far
                    container.ambiguity = idOccurrenceCount;
                    container.getBestIDSets().clear();
                }

                // if one of the best ID sets already contains [id1, id2], there's no need to add
                // anything like [id1, id2, id3]
                boolean idSetAlreadyContained = false;
                if (idOccurrenceCount == container.ambiguity) {
                    for (Collection<String> idSet : container.getBestIDSets()) {
                        if (subset.containsAll(idSet)) {
                            idSetAlreadyContained = true;
                            break;
                        }
                    }
                }

                // add ID set (unless the above condition applies)
                if (!idSetAlreadyContained)
                    container.getBestIDSets().add(subset);
            }
        }
    }

    /**
     * Checks whether the given layout (container) is suited for identification by its current best ID sets,
     * i.e. whether it can be removed from a list of all layouts to be checked
     * @param container      Container of the layout to check
     * @param currentSize    Size of subsets to consider (i.e. every subset with #currentSize IDs)
     * @return True if the layout is suited for identification (can be removed), false if not
     */
    private boolean checkRemoveContainer(LayoutIdentificationContainer container, int currentSize) {
        PowerSet<String> powerSet = container.getPowerSet();
        if (container.ambiguity == 1) {
            int bestSize = currentSize;
            for (Set<String> idSet : container.getBestIDSets())
                if (idSet.size() < bestSize) {
                    bestSize = idSet.size();
                    break;
                }
            // If the best size is two less than the current size, we can stop,
            // otherwise this takes too long. Although it never hurts to have
            // several bestIDSets, in case some elements (IDs) are invisible
            // on screen when comparing
            if (bestSize < currentSize - 1) {
                container.addBestIDSetsAsLayoutIdentifiers();
                return true;
            }
        }
        if (currentSize >= powerSet.getSetSize()) {
            if (container.ambiguity == 1) {
                container.addBestIDSetsAsLayoutIdentifiers();
            }
            return true;
        }
        return false;
    }

}
