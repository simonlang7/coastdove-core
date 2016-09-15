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
