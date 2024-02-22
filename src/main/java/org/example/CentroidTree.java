package org.example;

import java.util.ArrayList;

public class CentroidTree {
    private CentroidNode root;

    public CentroidTree(ASPNode aspRoot) {
        this.root = buildCentroidTree(aspRoot);
    }

    private CentroidNode buildCentroidTree(ASPNode aspNode) {
        if (aspNode == null || !aspNode.isHasChildren()) {
            return new CentroidNode(aspNode, 0);
        }

        // Find the centroid by comparing the distribution of points in each child
        ASPNode[] children = new ASPNode[]{aspNode.getNW(), aspNode.getNE(), aspNode.getSE(), aspNode.getSW()};
        int bestBalance = Integer.MAX_VALUE;
        ASPNode centroid = null;

        for (ASPNode child : children) {
            if (child != null) {
                int balance = aspNode.getCount() - child.getCount();
                if (Math.abs(balance) < bestBalance) {
                    bestBalance = Math.abs(balance);
                    centroid = child;
                }
            }
        }

        CentroidNode centroidNode = new CentroidNode(centroid, bestBalance);

        // Determine left and right children based on the point count
        CentroidNode leftChild = null;
        CentroidNode rightChild = null;

        for (ASPNode child : children) {
            if (child != centroid) {
                CentroidNode childCentroid = buildCentroidTree(child);
                if (leftChild == null || childCentroid.getPointCount() < leftChild.getPointCount()) {
                    rightChild = leftChild; // Previous leftChild is now rightChild
                    leftChild = childCentroid;
                } else if (rightChild == null || childCentroid.getPointCount() > rightChild.getPointCount()) {
                    rightChild = childCentroid;
                }
            }
        }

        centroidNode.setLeftChild(leftChild);
        centroidNode.setRightChild(rightChild);
        return centroidNode;
    }

    public ASPNode findSmallestBoxContainingPoint(double x, double y) {
        if (root == null) {
            return null;
        }
        return findSmallestBoxRecursively(root, x, y);
    }

    private ASPNode findSmallestBoxRecursively(CentroidNode node, double x, double y) {
        // Base case: if the node is null or does not contain the point, return null
        if (node == null || !node.getAspNode().getBounds().contains(x, y)) {
            return null;
        }

        // Check the left and right children to find which one contains the point
        ASPNode leftCandidate = findSmallestBoxRecursively(node.getLeftChild(), x, y);
        ASPNode rightCandidate = findSmallestBoxRecursively(node.getRightChild(), x, y);

        // If either child contains the point, return the smallest one
        if (leftCandidate != null && rightCandidate != null) {
            return leftCandidate.getBounds().area() < rightCandidate.getBounds().area() ? leftCandidate : rightCandidate;
        } else if (leftCandidate != null) {
            return leftCandidate;
        } else if (rightCandidate != null) {
            return rightCandidate;
        }

        // If no children contain the point, the current node is the smallest one that contains the point
        return node.getAspNode();
    }
    public void appendCentroidTreeOfRv(ASPNode v) {
        // Check if the subtree R(v) has already been constructed and is part of the ASP tree.
        if (!v.isHasChildren()) {
            // If v does not have children, there's no R(v) to append, so simply return.
            return;
        }

        // Assuming R(v) consists of v and its new children after refinement,
        // and that a new centroid tree needs to be built for this structure.
        CentroidNode newCentroidTreeRoot = buildCentroidTree(v);

        // Find the corresponding CentroidNode in the current centroid tree that represents v
        CentroidNode centroidNodeV = findCentroidNode(root, v);

        // Replace the old centroid node of v with the new subtree R(v) in the centroid tree.
        if (centroidNodeV != null && centroidNodeV.getParent() != null) {
            CentroidNode parent = centroidNodeV.getParent();
            if (parent.getLeftChild() == centroidNodeV) {
                parent.setLeftChild(newCentroidTreeRoot);
            } else if (parent.getRightChild() == centroidNodeV) {
                parent.setRightChild(newCentroidTreeRoot);
            }
        } else if (centroidNodeV != null) {
            // If v is the root of the centroid tree
            this.root = newCentroidTreeRoot;
        }

        // Update sizes of all ancestors in the centroid tree
        updateSubtreeSizes(centroidNodeV);

    }
    // Method to update subtree sizes starting from a specific node upwards
    private void updateSubtreeSizes(CentroidNode node) {
        // Traverse from the given node up to the root, updating sizes
        while (node != null) {
            // Assuming we have a method calculateSubtreeSize that computes the size of the subtree rooted at this node
            node.setSubtreeSize(calculateSubtreeSize(node));

            // Move to the parent node
            node = node.getParent();
        }
    }
    // Helper method to calculate the size of the subtree rooted at a given node
    private int calculateSubtreeSize(CentroidNode node) {
        if (node == null) return 0;
        int leftSize = calculateSubtreeSize(node.getLeftChild());
        int rightSize = calculateSubtreeSize(node.getRightChild());
        // Size of the subtree is the sum of sizes of left and right subtrees plus one for the current node
        return leftSize + rightSize + 1;
    }
    // Helper method to find the CentroidNode corresponding to an ASPNode v in the centroid tree
    private CentroidNode findCentroidNode(CentroidNode current, ASPNode target) {
        if (current == null) return null;
        if (current.getAspNode() == target) return current;

        CentroidNode foundInLeft = findCentroidNode(current.getLeftChild(), target);
        if (foundInLeft != null) return foundInLeft;

        CentroidNode foundInRight = findCentroidNode(current.getRightChild(), target);
        return foundInRight;
    }

    public void partialRebuilding(CentroidNode node) {
        // Check if the balance condition is violated at the node or its ancestors
        while (node != null) {
            if (needsRebalancing(node)) {
                // Rebuild the subtree rooted at node to restore balance
                rebuildSubtree(node);
            }
            // Move to the parent node to check balance condition upwards
            node = node.getParent();
        }
    }
    // Helper method to determine if a subtree rooted at a given node needs rebalancing
    private boolean needsRebalancing(CentroidNode node) {
        if (node == null) return false;
        int leftSize = node.getLeftChild() != null ? node.getLeftChild().getSubtreeSize() : 0;
        int rightSize = node.getRightChild() != null ? node.getRightChild().getSubtreeSize() : 0;
        int totalSize = leftSize + rightSize + 1; // +1 for the current node
        // Calculate the threshold as 1/8 of the total size of the subtree
        int threshold = totalSize / 8;
        // Define your rebalancing condition here. For example:
        return Math.abs(leftSize - rightSize) > threshold;
    }
    // Method to rebuild the subtree rooted at the given node
    private void rebuildSubtree(CentroidNode node) {
        // Collect all nodes in the subtree rooted at node
        ArrayList<CentroidNode> subtreeNodes = new ArrayList<>();
        collectSubtreeNodes(node, subtreeNodes);

        // Rebuild the subtree from collected nodes
        CentroidNode rebuiltSubtree = buildBalancedSubtree(subtreeNodes, 0, subtreeNodes.size() - 1);

        // Link the rebuilt subtree with the parent of the original node
        if (node.getParent() != null) {
            if (node.getParent().getLeftChild() == node) {
                node.getParent().setLeftChild(rebuiltSubtree);
            } else if (node.getParent().getRightChild() == node) {
                node.getParent().setRightChild(rebuiltSubtree);
            }
        } else {
            // If node was the root, update the root of the centroid tree
            this.root = rebuiltSubtree;
        }
    }
    // Helper method to collect all nodes in a subtree into a list
    private void collectSubtreeNodes(CentroidNode node, ArrayList<CentroidNode> nodes) {
        if (node == null) return;
        collectSubtreeNodes(node.getLeftChild(), nodes);
        nodes.add(node);
        collectSubtreeNodes(node.getRightChild(), nodes);
    }

    // Method to construct a balanced subtree from a list of nodes
    private CentroidNode buildBalancedSubtree(ArrayList<CentroidNode> nodes, int start, int end) {
        if (start > end) return null;
        int mid = (start + end) / 2;
        CentroidNode node = nodes.get(mid);

        // Recursively build left and right subtrees
        node.setLeftChild(buildBalancedSubtree(nodes, start, mid - 1));
        node.setRightChild(buildBalancedSubtree(nodes, mid + 1, end));
        node.setParent(null); // Clear parent, will be set by setLeftChild/setRightChild

        // Update subtree sizes
        node.setSubtreeSize(calculateSubtreeSize(node));
        return node;
    }

}
