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

package simonlang.coastdove.core.setup;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import brut.androlib.res.decoder.AXmlResourceParser;
import simonlang.coastdove.core.R;
import simonlang.coastdove.core.detection.AppDetectionData;
import simonlang.coastdove.core.detection.LayoutIdentification;
import simonlang.coastdove.core.ui.LoadingInfo;
import simonlang.coastdove.core.utility.APKToolHelper;
import simonlang.coastdove.core.utility.PowerSet;
import simonlang.coastdove.core.utility.Misc;
import simonlang.coastdove.lib.AppMetaInformation;
import simonlang.coastdove.lib.CollatorWrapper;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
public class AppDetectionDataSetup {
    /**
     * Constructs a new layout collection from the given .apk file
     * @param apkFile             APK file to process
     * @param minDetectionRate    minimal target detection rate (i.e. (#detectable layouts)/(#layouts)),
     *                            value between 0f and 1f
     */
    public static AppDetectionData fromAPK(Context context, File apkFile, String appPackageName, float minDetectionRate, LoadingInfo loadingInfo) {
        // Map (Layout -> Android IDs contained)
        Map<String, LayoutIdentification> layoutIdentificationMap = new HashMap<>();
        // Map (Android ID -> layouts in which it occurs)
        Map<String, Set<String>> reverseMap;
        // Set of all "android:id" attributes that occur throughout all layouts
        Set<String> allAndroidIDs = new TreeSet<>(Collator.getInstance());
        // Set of activities available from the Android launcher
        Set<String> mainActivities = new TreeSet<>(new CollatorWrapper());

        loadingInfo.setNotificationData(context.getString(R.string.add_app_notification_loading),
                appPackageName, R.drawable.notification_template_icon_bg);
        loadingInfo.setTitle(context.getString(R.string.add_app_parsing_resources));
        loadingInfo.start(true);

        ZipFile apk;
        try {
            apk = new ZipFile(apkFile);
        } catch (IOException e) {
            Log.e("AppDetectionDataSetup", "Cannot open ZipFile: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        Log.d("AppDetectionDataSetup", "Parsing resources.arsc and AndroidManifest.xml");
        ARSCFileParser resourceParser = new ARSCFileParser();
        ZipEntry arscEntry = apk.getEntry("resources.arsc");
        APKToolHelper apktoolHelper = new APKToolHelper(apkFile);
        InputStream manifestInputStream = apktoolHelper.manifestInputStream();
        //byte[] manifestBuf = APKToolHelper.decodeManifestWithResources(context, apkFile);
        //ByteArrayInputStream manifestInputStream = new ByteArrayInputStream(manifestBuf);

        try {
            resourceParser.parse(apk.getInputStream(arscEntry));
            if (Thread.currentThread().isInterrupted()) {
                loadingInfo.end();
                apk.close();
                return null;
            }

            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser p = fac.newPullParser();
            p.setInput(manifestInputStream, "UTF-8");
            mainActivities = parseMainActivities(p);
            manifestInputStream.close();
            if (Thread.currentThread().isInterrupted()) {
                loadingInfo.end();
                apk.close();
                return null;
            }
        } catch (IOException e) {
            Log.e("AppDetectionDataSetup", "Cannot get InputStream: " + e.getMessage());
        } catch (XmlPullParserException e) {
            Log.e("AppDetectionDataSetup", "Cannot get XmlPullParser: " + e.getMessage());
        }

        AppMetaInformation appMetaInformation = new AppMetaInformation(appPackageName, mainActivities);

        // Read APK file
        loadingInfo.setTitle(context.getString(R.string.add_app_reading_apk));
        loadingInfo.update();
        Log.d("AppDetectionDataSetup", "Reading APK file");
        Enumeration<?> zipEntries = apk.entries();
        List<LayoutIdentificationContainer> containers = new LinkedList<>();
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry)zipEntries.nextElement();
            try {
                if (Thread.currentThread().isInterrupted()) {
                    loadingInfo.end();
                    apk.close();
                    return null;
                }
                String entryName = zipEntry.getName();
                if (entryName.contains("res/layout/") && Misc.isBinaryXML(apk.getInputStream(zipEntry))) {
                    String name = entryName.replaceAll(".*/", "").replace(".xml", "");

                    AXmlResourceParser parser = new AXmlResourceParser();
                    parser.open(apk.getInputStream(zipEntry));
                    Set<String> androidIDs = parseAndroidIDs(parser, resourceParser);

                    LayoutIdentificationContainer container = new LayoutIdentificationContainer(name, androidIDs);
                    layoutIdentificationMap.put(name, container.getLayoutIdentification());
                    containers.add(container);

                    allAndroidIDs.addAll(container.getAndroidIDs());
                }
            } catch (IOException e) {
                Log.e("AppDetectionDataSetup", "Error reading APK file: " + e.getMessage());
            }
        }

        try {
            apk.close();
        } catch (IOException e) {
            Log.e("AppDetectionDataSetup", "Unable to close APK file: " + e.getMessage());
        }

        // get unique IDs and reverse map
        List<LayoutIdentificationContainer> containersCopy = new LinkedList<>(containers);
        loadingInfo.setTitle(context.getString(R.string.add_app_setting_up_layouts));
        loadingInfo.update();
        int accuracy = lookupUniqueIDs(containersCopy, loadingInfo);
        reverseMap = buildReverseMap(containers, allAndroidIDs);

        loadingInfo.setNotificationData(context.getString(R.string.add_app_notification_finished_loading),
                null, null);
        loadingInfo.end();

        return new AppDetectionData(appPackageName, layoutIdentificationMap, reverseMap, appMetaInformation, accuracy);
    }

    /**
     * Parses all "android:id" values using the given XmlResourceParser. The parser must be opened before
     * calling this function.
     * @param parser            Parser to parse the data from
     * @param resourceParser    ARSC parser to parse resource strings from, in case the XML is parsed from binary.
     *                          If null, resource strings are not replaced.
     * @return Set of all "android:id" values occurring in the XML file
     */
    private static Set<String> parseAndroidIDs(XmlPullParser parser, @Nullable ARSCFileParser resourceParser) {
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
            Log.e("AppDetectionDataSetup", "Unable to parse from XML file: " + e.getMessage());
        } catch (IOException e) {
            Log.e("AppDetectionDataSetup", "IO error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Extracts activities available from the Android Launcher
     * @param parser    Parser to parse the data from
     * @return Set of main activities
     */
    private static Set<String> parseMainActivities(XmlPullParser parser) {
        Set<String> mainActivites = new TreeSet<>(new CollatorWrapper());
        try {
            String lastActivity = null;
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG)
                    continue;
                if (parser.getName().equals("activity")) {
                    for (int i = 0; i < parser.getAttributeCount(); ++i) {
                        if (parser.getAttributeName(i).equals("android:name"))
                            lastActivity = parser.getAttributeValue(i);
                    }
                }
                else if (parser.getName().equals("action")) {
                    for (int i = 0; i < parser.getAttributeCount(); ++i) {
                        if (parser.getAttributeName(i).equals("android:name") &&
                                parser.getAttributeValue(i) != null &&
                                parser.getAttributeValue(i).contains("android.intent.action.MAIN") &&
                                lastActivity != null) {
                            mainActivites.add(lastActivity);
                        }
                    }
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Log.e("AppDetectionDataSetup", "Unable to parse from XML file: " + e.getMessage());
        }

        return mainActivites;
    }

    /**
     * Given a set of layouts (containers), this function looks at all "android:id" attributes in
     * each of them, and finds sets of androidIDs that are best suited for recognizing the layout,
     * i.e. sets that uniquely identify a layout. The final data is saved in each LayoutIdentification
     * which is held in its own container
     * @param containers    Layouts (containers) to process
     */
    private static int lookupUniqueIDs(List<LayoutIdentificationContainer> containers, LoadingInfo loadingInfo) {
        int layoutsCount = containers.size();
        loadingInfo.setContentText("0%");
        loadingInfo.setIndeterminate(false);
        int progress = 0;
        loadingInfo.update(100, progress);

        // The fewer androidIDs we need to identify a layout, the better, hence start with size 1
        outer: for (int currentSize = 1; currentSize < 16 && containers.size() > 0; ++currentSize) {
            Log.d("AppDetectionDataSetup", "Size: " + currentSize);
            Map<String, Integer> idOccurrenceCounts = idOccurrenceCounts(containers, currentSize);

            int count = 1;
            int currentMax = containers.size();
            // What's the current "best" for identifiers?
            Iterator<LayoutIdentificationContainer> it = containers.iterator();
            while (it.hasNext()) {
                if (Thread.interrupted()) { // This clears the interrupt flag (unlike Thread.currentThread().isInterrupted())
                    if (progress < 50) // If progress too low, interrupt thread again, so the multiLoader does not add the resulting data
                        Thread.currentThread().interrupt();
                    break outer;
                }
                if (currentSize >= 5)
                    Log.d("AppDetectionDataSetup", count + " / " + currentMax);

                LayoutIdentificationContainer container = it.next();

                updateBestIDSets(container, currentSize, idOccurrenceCounts);
                boolean removeContainer = checkRemoveContainer(container, currentSize);
                if (removeContainer)
                    it.remove();

                ++count;
            }

            float accuracy = (layoutsCount - containers.size())/(float)layoutsCount;
            progress = Math.round(accuracy * 100);
            loadingInfo.setContentText(progress + "%");
            loadingInfo.update(100, progress);
            Log.d("AppDetectionDataSetup", "Accuracy: " + (layoutsCount - containers.size())/(float)layoutsCount);
        }

        // Check the leftovers
        for (LayoutIdentificationContainer container : containers) {
            if (container.ambiguity == 1) {
                container.addBestIDSetsAsLayoutIdentifiers();
            }
        }

        return progress;
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
    private static HashMap<String, Integer> idOccurrenceCounts(List<LayoutIdentificationContainer> containers, int currentSize) {
        HashMap<String, Integer> result = new HashMap<>();
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
     *                              from {@link AppDetectionDataSetup#idOccurrenceCounts(List, int)}
     */
    private static void updateBestIDSets(LayoutIdentificationContainer container, int currentSize,
                                  Map<String, Integer> idOccurrenceCounts) {
        PowerSet<String> powerSet = container.getPowerSet();
        powerSet.startOver(currentSize);

        while (powerSet.hasNext() && powerSet.sizeOfNext() == currentSize) {
            if (Thread.currentThread().isInterrupted())
                return;
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
    private static boolean checkRemoveContainer(LayoutIdentificationContainer container, int currentSize) {
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

    /**
     * Builds the reverse map, mapping from (android ID -> possible Layouts)
     * @param containers    Layout identification containers, as found after calling lookupUniqueIDs
     * @param allIDs        All Android IDs from all layouts
     * @return Reverse map, mapping from Android IDs to a list of layouts that can possibly be detected
     */
    private static Map<String, Set<String>> buildReverseMap(List<LayoutIdentificationContainer> containers, Set<String> allIDs) {
        Map<String, Set<String>> result = new HashMap<>(allIDs.size());

        for (String id : allIDs) {
            Set<String> possibleLayouts = new TreeSet<>(new CollatorWrapper());
            for (LayoutIdentificationContainer container : containers) {
                if (container.getAndroidIDs().contains(id))
                    possibleLayouts.add(container.getName());
            }
            result.put(id, possibleLayouts);
        }

        return result;
    }

}
