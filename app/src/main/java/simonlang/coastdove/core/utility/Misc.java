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

package simonlang.coastdove.core.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import simonlang.coastdove.core.CoastDoveService;
import simonlang.coastdove.core.detection.ReplacementData;

/**
 * A collection of general utility functions
 */
public class Misc {
    // Date / Time
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String DATE_TIME_FILENAME = "yyyy-MM-dd_HH-mm-ss_SSS";

    // Scroll positions
    public static final String ADD_APP_SCROLL_POSITION_PREF = "scroll_position_add_app";


    /**
     * Converts milliseconds to a String of the format "[Hh ][Mm ]Ss", e.g., "1h 20m 3s", "45m 22s" or "54s"
     * @param ms    Duration in milliseconds
     */
    public static String msToDurationString(long ms) {
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(minutes);
        String hoursString = hours == 0 ? "" : hours + "h ";
        String minutesString = minutes == 0 ? "" : minutes + "m ";
        String secondsString = seconds == 0 ? "< 1s" : seconds + "s";
        return hoursString + minutesString + secondsString;
    }

    /**
     * Sets and commits the given preference (appPackageName+preference) with the given value
     * @param preferences       Shared preferences to commit to
     * @param appPackageName    Package name for which to set the preference
     * @param preference        Preference name to set
     * @param value             Desired value of the preference
     */
    public static void setPreference(SharedPreferences preferences, String appPackageName, String preference,
                                     boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(appPackageName + preference, value);
        editor.commit();
    }

    /**
     * Retrieves the given preference (appPackageName+preference)
     * @param preferences       Shared preferences to retrieve from
     * @param appPackageName    Package name for which to retrieve the preference
     * @param preference        Preference name to get
     * @param defaultValue      Default value if the preference is not set
     * @return The preference's value
     */
    public static boolean getPreferenceBoolean(SharedPreferences preferences, String appPackageName, String preference,
                                               boolean defaultValue) {
        return preferences.getBoolean(appPackageName + preference, defaultValue);
    }

    /**
     * Indicated whether any of this package's accessibility services is currently active
     * @param context    App context
     * @return True if an accessibility service of this packge is currently active
     */
    public static boolean isAccessibilityServiceActive(Context context) {
        String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String[] activeAccessibilityServices = settingValue != null ? settingValue.split(":") : new String[0];
        for (String service : activeAccessibilityServices) {
            if (service.contains(CoastDoveService.class.getName()))
                return true;
        }
        return false;
    }

    /**
     * Loads replacement data from files for the given app
     * @return ReplacementData with the saved replacement mapping (or a new one if none existed), or
     *         null if no replacement data were found on the device
     */
    public static ReplacementData loadReplacementData(Context context, String appPackageName) {
        ReplacementData replacementData = null;
        JSONObject replacementDataJSON = FileHelper.readJSONFile(context, FileHelper.Directory.PUBLIC_PACKAGE, appPackageName, FileHelper.REPLACEMENT_DATA);
        if (replacementDataJSON != null) {
            replacementData = ReplacementData.fromJSON(replacementDataJSON);
            File replacementMapFile = FileHelper.getFile(context, FileHelper.Directory.PRIVATE_PACKAGE, appPackageName, FileHelper.REPLACEMENT_MAP);
            if (replacementMapFile != null && replacementMapFile.exists()) {
                HashMap<String, Integer> replacementMap = (HashMap<String, Integer>) FileHelper.readHashMap(context,
                        FileHelper.Directory.PRIVATE_PACKAGE, appPackageName, FileHelper.REPLACEMENT_MAP);
                replacementData.setReplacementMap(replacementMap);
            }
        }
        return replacementData;
    }

    /**
     * Parses an XML file
     * @param inputStream    Input stream to parse from
     * @return The XML as a Document
     */
    public static Document parseXMLFile(InputStream inputStream) {
        Document result;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            result = db.parse(inputStream);
        } catch (Exception e) {
            Log.e("XMLHelper", "Cannot parse XML: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        return result;
    }

    /**
     * Determines whether the data from the given InputStream is a binary XML
     * Code taken from apk2xml by Ivo Hradek, licensed under the Apache License 2.0
     * @param stream    Stream to read from
     * @return True if the stream contains a binary XML, false if not
     * @throws IOException If the stream cannot be read
     */
    public static boolean isBinaryXML(InputStream stream) throws IOException {
        byte[] expect = new byte[]{0x03, 0x00, 0x08, 0x00};
        byte[] magic = new byte[4];
        stream.read(magic);
        return Arrays.equals(magic, expect);
    }
}
