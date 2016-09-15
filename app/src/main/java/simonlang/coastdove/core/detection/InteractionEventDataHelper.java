package simonlang.coastdove.core.detection;

import android.content.ContentValues;
import android.view.accessibility.AccessibilityNodeInfo;

import simonlang.coastdove.core.usage.sql.AppUsageContract;
import simonlang.coastdove.lib.InteractionEventData;

/**
 * Helper class for InteractionEventData to provide additional import
 * and export methods
 */
public class InteractionEventDataHelper {
    /**
     * Constructs an InteractionEventData object from the given node info, replacing private data
     * where necessary
     * @param nodeInfo           Node info to construct the object from, as retrieved from an AccessibilityEvent
     * @param replacementData    Rules to replace private data
     */
    public static InteractionEventData fromAccessibilityNodeInfo(AccessibilityNodeInfo nodeInfo, ReplacementData replacementData) {
        String androidID = nodeInfo.getViewIdResourceName() != null ? nodeInfo.getViewIdResourceName() : "";
        String text = nodeInfo.getText() != null ? nodeInfo.getText().toString().replaceAll("\n", " ") : "";
        String description = nodeInfo.getContentDescription() != null ? nodeInfo.getContentDescription().toString().replaceAll("\n", " ") : "";
        String className = nodeInfo.getClassName() != null ? nodeInfo.getClassName().toString() : "";

        if (replacementData != null && replacementData.hasReplacementRule(androidID)) {
            ReplacementData.ReplacementRule rule = replacementData.getReplacementRule(androidID);
            if (rule.replaceText != null) {
                switch (rule.replaceText) {
                    case DISCARD:
                        text = "";
                        break;
                    case REPLACE:
                        text = replacementData.getReplacement(text);
                        break;
                }
            }
            if (rule.replaceDescription != null) {
                switch (rule.replaceDescription) {
                    case DISCARD:
                        description = "";
                        break;
                    case REPLACE:
                        description = replacementData.getReplacement(description);
                        break;
                }
            }
        }

        return new InteractionEventData(androidID, text, description, className);
    }


    /**
     * Converts an InteractionEventData object into ContentValues for its SQLite representation
     * @param data           InteractionEventData to convert
     * @param dataEntryID    Primary key of the associated ActivityDataEntry
     * @return ContentValues representing the original object
     */
    public static ContentValues toContentValues(InteractionEventData data, long dataEntryID) {
        ContentValues values = new ContentValues();
        values.put(AppUsageContract.InteractionDetailsTable.COLUMN_NAME_ANDROID_ID, data.getAndroidID());
        values.put(AppUsageContract.InteractionDetailsTable.COLUMN_NAME_TEXT, data.getText());
        values.put(AppUsageContract.InteractionDetailsTable.COLUMN_NAME_DESCRIPTION, data.getDescription());
        values.put(AppUsageContract.InteractionDetailsTable.COLUMN_NAME_CLASS_NAME, data.getClassName());
        values.put(AppUsageContract.InteractionDetailsTable.COLUMN_NAME_DATA_ENTRY_ID, dataEntryID);
        return values;
    }
}
