package simonlang.coastdove.core.detection;

import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import simonlang.coastdove.lib.ViewTreeNode;

/**
 * Helper class to construct view trees
 */
public abstract class ViewTreeHelper {
    /**
     * Creates a ViewTreeNode that represents an entire subtree of AccessibilityNodeInfos
     * @param rootNodeInfo    AccessibilityNodeInfo to start from, i.e., the root of the subtree
     * @param replacementData    to replace private data
     * @return Copied representation of an AccessibilityNodeInfo subtree
     */
    public static ViewTreeNode fromAccessibilityNodeInfo(AccessibilityNodeInfo rootNodeInfo,
                                                         ReplacementData replacementData) {
        Stack<AccessibilityNodeInfo> nodeInfos = new Stack<>();
        nodeInfos.push(rootNodeInfo);
        Stack<ViewTreeNode> viewTreeNodes = new Stack<>();

        // Traverse tree with depth-first search
        AccessibilityNodeInfo currentNodeInfo;
        ViewTreeNode currentViewTreeNode;
        while (!nodeInfos.empty()) {
            currentNodeInfo = nodeInfos.pop();

            // Have we just finished traversing all children?
            if (currentNodeInfo == null) {
                List<ViewTreeNode> children = new LinkedList<>();

                // Collect children
                currentViewTreeNode = viewTreeNodes.pop();
                while (currentViewTreeNode != null) { // until we hit the parent
                    children.add(currentViewTreeNode);
                    currentViewTreeNode = viewTreeNodes.pop();
                }

                // Add children to parent, link parent to children
                ViewTreeNode parent = viewTreeNodes.peek();
                for (ViewTreeNode child : children) {
                    child.setParent(parent);
                    parent.getChildren().add(child);
                }
            }
            else { // or did we just start processing a new node?
                int childCount = currentNodeInfo.getChildCount();

                // Process the current node (without children)
                currentViewTreeNode = flatCopy(currentNodeInfo, replacementData);

                // Add node to stack
                viewTreeNodes.add(currentViewTreeNode);

                if (childCount > 0) {
                    // Signal that this node has children
                    nodeInfos.push(null);
                    viewTreeNodes.push(null);
                    for (int i = 0; i < childCount; ++i)
                        nodeInfos.push(currentNodeInfo.getChild(i));
                }
            }
        }

        // Once the nodeInfo stack is empty, there should be exactly one element
        // left on the ViewTreeNode stack.
        return viewTreeNodes.pop();
    }

    /**
     * Copies all required data from an AccessibilityNodeInfo into a newly
     * created ViewTreeNode, omitting parents and children
     * @param nodeInfo           AccessibilityNodeInfo to copy from
     * @param replacementData    to replace private data
     */
    public static ViewTreeNode flatCopy(AccessibilityNodeInfo nodeInfo,
                                        ReplacementData replacementData) {
        ViewTreeNode result = new ViewTreeNode();
        result.setParent(null);
        result.setChildren(new ArrayList<ViewTreeNode>(nodeInfo.getChildCount()));

        String androidID = nodeInfo.getViewIdResourceName();
        String text = charSeqToString(nodeInfo.getText());
        String description = charSeqToString(nodeInfo.getContentDescription());
        if (replacementData.hasReplacementRule(androidID)) {
            ReplacementData.ReplacementRule rule = replacementData.getReplacementRule(androidID);
            switch (rule.replaceText) {
                case REPLACE:
                    result.setText(replacementData.getReplacement(text));
                    break;
                case DISCARD:
                    result.setText("");
                    break;
                default:
                    result.setText(text);

            }
            switch (rule.replaceDescription) {
                case REPLACE:
                    result.setContentDescription(replacementData.getReplacement(description));
                    break;
                case DISCARD:
                    result.setContentDescription("");
                    break;
                default:
                    result.setContentDescription(description);
            }
        }
        else {
            result.setText(text);
            result.setContentDescription(description);
        }

        result.setClassName(charSeqToString(nodeInfo.getClassName()));
        result.setInputType(nodeInfo.getInputType());
        result.setTextSelectionStart(nodeInfo.getTextSelectionStart());
        result.setTextSelectionEnd(nodeInfo.getTextSelectionEnd());
        result.setViewIDResourceName(nodeInfo.getViewIdResourceName());

        if (Build.VERSION.SDK_INT >= 21)
            result.setActionList(new LinkedList<>(nodeInfo.getActionList()));
        else
            result.setActionList(new LinkedList<AccessibilityNodeInfo.AccessibilityAction>());

        result.setCheckable(nodeInfo.isCheckable());
        result.setChecked(nodeInfo.isChecked());
        result.setClickable(nodeInfo.isClickable());
        result.setDismissable(nodeInfo.isDismissable());
        result.setEditable(nodeInfo.isEditable());
        result.setEnabled(nodeInfo.isEnabled());
        result.setFocusable(nodeInfo.isFocusable());
        result.setFocused(nodeInfo.isFocused());
        result.setLongClickable(nodeInfo.isLongClickable());
        result.setMultiLine(nodeInfo.isMultiLine());
        result.setPassword(nodeInfo.isPassword());
        result.setScrollable(nodeInfo.isScrollable());
        result.setSelected(nodeInfo.isSelected());
        result.setVisibleToUser(nodeInfo.isVisibleToUser());

        return result;
    }

    private static String charSeqToString(CharSequence seq) {
        return seq == null ? null : seq.toString();
    }
}
