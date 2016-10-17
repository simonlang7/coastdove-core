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

package simonlang.coastdove.core.detection;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import brut.androlib.res.decoder.AXmlResourceParser;
import simonlang.coastdove.core.R;
import simonlang.coastdove.core.ui.LoadingInfo;
import simonlang.coastdove.core.utility.APKToolHelper;
import simonlang.coastdove.core.utility.Misc;
import simonlang.coastdove.lib.AppMetaInformation;
import simonlang.coastdove.lib.CollatorWrapper;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.Enumeration;
import java.util.HashMap;
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
     */
    public static AppDetectionData fromAPK(Context context, File apkFile, String appPackageName, LoadingInfo loadingInfo) {
        // Map if how often IDs occur
        Map<String, Integer> idCounts = new HashMap<>();
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

        List<Pair<String, Set<String>>> layoutIDSets = new LinkedList<>();
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

                    layoutIDSets.add(new Pair<>(name, androidIDs));

                    // Count all IDs
                    for (String id : androidIDs) {
                        if (idCounts.containsKey(id)) {
                            int count = idCounts.get(id);
                            idCounts.put(id, count+1);
                        }
                        else
                            idCounts.put(id, 1);
                    }
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
        loadingInfo.setTitle(context.getString(R.string.add_app_setting_up_layouts));
        loadingInfo.update();

        // Build the final map we actually need (id -> layout)
        Map<String, String> idToLayoutMap = new HashMap<>();
        for (Pair<String, Set<String>> layoutIDSet : layoutIDSets) {
            for (String id : layoutIDSet.second) {
                if (idCounts.get(id) == 1)
                    idToLayoutMap.put(id, layoutIDSet.first);
            }
        }

        loadingInfo.setNotificationData(context.getString(R.string.add_app_notification_finished_loading),
                null, null);
        loadingInfo.end();

        return new AppDetectionData(appPackageName, idToLayoutMap, appMetaInformation);
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
}
