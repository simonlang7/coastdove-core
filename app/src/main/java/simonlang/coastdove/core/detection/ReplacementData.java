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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Data for replacing the content of elements (identified by androidIDs) on the screen
 */
public class ReplacementData {
    /**
     * Indicates how to replace content
     */
    public enum ReplacementType {
        /** Discard the content (i.e., replace by "") */
        DISCARD,
        /** Replace the content by a fixed String (e.g., "Josh" will always be replaced by "1") */
        REPLACE,
        /** Keep the original content */
        KEEP
    }

    /**
     * Indicates for one element how to replace its contents
     */
    public static class ReplacementRule {
        /** Indicates how to replace the text content */
        public final ReplacementType replaceText;
        /** Indicates how to replace the description content */
        public final ReplacementType replaceDescription;

        public ReplacementRule(ReplacementType replaceText, ReplacementType replaceDescription) {
            this.replaceText = replaceText;
            this.replaceDescription = replaceDescription;
        }
    }

    /**
     * Creates replacement data for an app from a JSON object
     * @param data    JSON objects containing replacement data
     * @return Object containing replacement data
     */
    public static ReplacementData fromJSON(JSONObject data) {
        ReplacementData result = new ReplacementData();
        try {
            ReplacementType notificationReplacement = ReplacementType.KEEP;
            if (data.has("notifications"))
                notificationReplacement = ReplacementType.valueOf(data.getString("notifications"));
            result.notificationReplacement = notificationReplacement;

            JSONArray replacementDataArray = data.getJSONArray("replacementData");
            for (int i = 0; i < replacementDataArray.length(); ++i) {
                JSONObject replacementEntry = replacementDataArray.getJSONObject(i);
                String androidID = replacementEntry.getString("androidID");
                JSONObject replacementJSON = replacementEntry.getJSONObject("replacement");

                ReplacementType replaceText = null;
                ReplacementType replaceDescription = null;
                if (replacementJSON.has("text"))
                    replaceText = ReplacementType.valueOf(replacementJSON.getString("text"));
                if (replacementJSON.has("description"))
                    replaceDescription = ReplacementType.valueOf(replacementJSON.getString("description"));

                result.replacementRules.put(androidID, new ReplacementRule(replaceText, replaceDescription));
            }
        } catch (JSONException e) {
            throw new RuntimeException("Cannot read from JSON: " + e.getMessage());
        }

        return result;
    }

    /** Map (androidID -> ReplacementRule) */
    private Map<String, ReplacementRule> replacementRules;

    /** Map (actual String -> substituted Integer) */
    private HashMap<String, Integer> replacementMap;

    /** How to replace notifications */
    private ReplacementType notificationReplacement;

    /** Whether the replacement map has changed since being last  */
    private boolean changed;

    /** Next number for substitution (the first string is replaced by 0, the second by 1, ...) */
    private int nextSubstitution;

    /** Returns the replacement rule for elements with the given androidID as the key */
    public ReplacementRule getReplacementRule(String key) {
        return replacementRules.get(key);
    }

    /** Indicates whether there is a replacement rule for the given androidID as the key */
    public boolean hasReplacementRule(String key) {
        return replacementRules.containsKey(key);
    }

    /** How to replace notifications */
    public ReplacementType getNotificationReplacement() {
        return notificationReplacement;
    }

    /**
     * Returns the internal replacement map (actualString -> substituted Integer)
     */
    public HashMap<String, Integer> getReplacementMap() {
        return this.replacementMap;
    }

    /**
     * Sets the replacement map (actual String -> substituted Integer), used when loading this
     * map from binary
     * @param replacementMap    Replacement map to set
     */
    public void setReplacementMap(HashMap<String, Integer> replacementMap) {
        this.replacementMap = replacementMap;
        this.nextSubstitution = replacementMap.size();
    }

    /**
     * Retrieves the replacement string for the given string
     * @param key    String to replace
     * @return Replacement string
     */
    public String getReplacement(String key) {
        if (key.equals(""))
            return "";

        if (!replacementMap.containsKey(key)) {
            replacementMap.put(key, nextSubstitution);
            ++nextSubstitution;
            changed = true;
        }
        return ""+replacementMap.get(key);
    }

    /** Whether the replacement map has changed since being last */
    public boolean hasChanged() {
        return this.changed;
    }

    /** Sets the changed status for the replacement map */
    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    /**
     * Constructs a replacement data object
     */
    private ReplacementData() {
        this.notificationReplacement = ReplacementType.KEEP;
        this.replacementRules = new HashMap<>();
        this.replacementMap = new HashMap<>();
        this.nextSubstitution = 0;
        this.changed = false;
    }
}
