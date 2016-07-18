package de.uni_bonn.detectappscreen;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Class to filter node infos according to specified rules
 */
public abstract class NodeInfoFilter {
    /**
     * Indicates whether the given node info applies to the
     * implemented filter or not.
     * @param nodeInfo    Node info to be filtered
     * @return True if the rule applies to the node info, false otherwise.
     */
    public abstract boolean filter(AccessibilityNodeInfo nodeInfo);
}