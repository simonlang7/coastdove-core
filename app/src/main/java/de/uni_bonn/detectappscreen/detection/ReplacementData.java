package de.uni_bonn.detectappscreen.detection;

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
        REPLACE
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

    private boolean changed;

    private int nextSubstitution;

    /** Returns the replacement rule for elements with the given androidID as the key */
    public ReplacementRule getReplacementRule(String key) {
        return replacementRules.get(key);
    }

    /** Indicates whether there is a replacement rule for the given androidID as the key */
    public boolean hasReplacementRule(String key) {
        return replacementRules.containsKey(key);
    }

    /**
     * Returns the internal replacement map (actualString -> substituted Integer)
     */
    public HashMap<String, Integer> getReplacementMap() {
        return this.replacementMap;
    }

    public void setReplacementMap(HashMap<String, Integer> replacementMap) {
        this.replacementMap = replacementMap;
        this.nextSubstitution = replacementMap.size();
    }

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

    public boolean hasChanged() {
        return this.changed;
    }

    private ReplacementData() {
        this.replacementRules = new HashMap<>();
        this.replacementMap = new HashMap<>();
        this.nextSubstitution = 0;
        this.changed = false;
    }
}
