package de.uni_bonn.detectappscreen;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Class to extract data from a node info
 */
public abstract class NodeInfoDataExtractor<T> {
    /**
     * Extracts data from a given node info.
     * @param nodeInfo    Node info to extract data from
     * @return Data extracted
     */
    public abstract T extractData(AccessibilityNodeInfo nodeInfo);
}