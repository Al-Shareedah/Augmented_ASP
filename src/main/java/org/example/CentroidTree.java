package org.example;
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

}
