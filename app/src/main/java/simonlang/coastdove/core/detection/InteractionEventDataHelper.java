package simonlang.coastdove.core.detection;

import android.view.accessibility.AccessibilityNodeInfo;

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

}
