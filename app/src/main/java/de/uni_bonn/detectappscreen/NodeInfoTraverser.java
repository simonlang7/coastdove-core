package de.uni_bonn.detectappscreen;

import android.view.accessibility.AccessibilityNodeInfo;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Performs a breadth-first search on a tree of AccessibilityNodeInfo objects,
 * returning all nodes that match a certain criterion
 */
public class NodeInfoTraverser<T> {
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


    /** Node info from which to start traversing the tree */
    private AccessibilityNodeInfo startNodeInfo;
    /** Queue of node infos needed for processing */
    private Queue<AccessibilityNodeInfo> nodeInfos;
    /** Filters node infos according to its rules */
    private NodeInfoFilter nodeInfoFilter;
    /** Extracts data from node infos according to its rules */
    private NodeInfoDataExtractor<T> dataExtractor;

    /**
     * Initialize node info traverser with the given start node. No siblings or parents
     * (if any) of the start node are processed.
     * @param startNodeInfo    Node that is treated as the root node of the tree
     * @param dataExtractor    Rules for data extraction
     */
    public NodeInfoTraverser(AccessibilityNodeInfo startNodeInfo, NodeInfoDataExtractor<T> dataExtractor) {
        if (dataExtractor == null)
            throw new IllegalArgumentException("dataExtractor must not be null");
        this.startNodeInfo = startNodeInfo;
        this.dataExtractor = dataExtractor;
        this.nodeInfoFilter = null;
        reset();
    }

    /**
     * Initialize node info traverser with the given start node. No siblings or parents
     * (if any) of the start node are processed.
     * @param startNodeInfo    Node that is treated as the root node of the tree
     * @param dataExtractor    Rules for data extraction
     * @param filter           Rules for filtering processed nodes
     */
    public NodeInfoTraverser(AccessibilityNodeInfo startNodeInfo, NodeInfoDataExtractor<T> dataExtractor,
                             NodeInfoFilter filter) {
        if (dataExtractor == null)
            throw new IllegalArgumentException("dataExtractor must not be null");
        this.startNodeInfo = startNodeInfo;
        this.dataExtractor = dataExtractor;
        this.nodeInfoFilter = filter;
        reset();
    }

    /**
     * Start over with the first element
     */
    public void reset() {
        this.nodeInfos = new LinkedList<>();
        this.nodeInfos.add(this.startNodeInfo);
    }

    /**
     * @return True if there is at least one element left in the tree,
     * false otherwise
     */
    public boolean hasNext() {
        return this.nodeInfos.size() > 0;
    }

    /**
     * Extracts data from the next node info in the tree. Does not take the
     * filter() rule into account
     * @return The data extracted from the next node info in the tree
     */
    public T next() {
        AccessibilityNodeInfo currentNodeInfo = nextNodeInfo();
        if (currentNodeInfo != null)
            return extractData(currentNodeInfo);
        else
            return null;
    }

    /**
     * Extracts data from all nodes left in the tree. Does not take the
     * filter() rule into account. Resets the traverser afterwards.
     * @return List of data extracted from each node
     */
    public List<T> getAll() {
        List<T> result = new LinkedList<>();
        while (hasNext())
            result.add(next());
        reset();
        return result;
    }

    /**
     * Extracts data from all nodes left in the tree to which the rules of
     * {@link NodeInfoTraverser#filter(AccessibilityNodeInfo)} apply.
     * Resets the traverser afterwards.
     * @return List of data extracted from each node to which the filter() rules apply
     */
    public List<T> getAllFiltered() {
        List<T> result = new LinkedList<T>();
        while (hasNext()) {
            AccessibilityNodeInfo currentNodeInfo = nextNodeInfo();
            if (filter(currentNodeInfo))
                result.add(extractData(currentNodeInfo));
        }
        reset();
        return result;
    }

    /**
     * Extracts data from a given node info. Meant to be overridden
     * by inheriting class.
     * @param nodeInfo    Node info to extract data from
     * @return Data extracted
     */
    private T extractData(AccessibilityNodeInfo nodeInfo) {
        return dataExtractor.extractData(nodeInfo);
    }

    /**
     * Indicates whether the given node info applies to the
     * implemented filter or not.
     * @param nodeInfo    Node info to be filtered
     * @return True if the rule applies to the node info, false otherwise.
     */
    private boolean filter(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfoFilter != null)
            return nodeInfoFilter.filter(nodeInfo);
        else
            return true;
    }

    /**
     * Adds all children of the the given node info to the internal
     * node info queue.
     * @param nodeInfo    Node info from which to add children
     */
    private void addChildren(AccessibilityNodeInfo nodeInfo) {
        for (int i = 0; i < nodeInfo.getChildCount(); ++i)
            this.nodeInfos.add(nodeInfo.getChild(i));
    }

    /**
     * Returns the next node info in the tree
     * @return Next node info in the tree
     */
    private AccessibilityNodeInfo nextNodeInfo() {
        AccessibilityNodeInfo currentNodeInfo = this.nodeInfos.poll();
        if (currentNodeInfo != null) {
            addChildren(currentNodeInfo);
            return currentNodeInfo;
        }
        else
            return null;
    }

    public void setNodeInfoFilter(NodeInfoFilter nodeInfoFilter) {
        this.nodeInfoFilter = nodeInfoFilter;
    }

    public void setDataExtractor(NodeInfoDataExtractor<T> dataExtractor) {
        if (dataExtractor == null)
            throw new IllegalArgumentException("dataExtractor must not be null");
        this.dataExtractor = dataExtractor;
    }
}
