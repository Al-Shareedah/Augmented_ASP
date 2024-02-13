package org.example;
import util.Box;

public class ASPTree {
    protected ASPNode root = null;
    private CentroidTree centroidTree;
    private int size_n = 0;
    /**
     * Creates an empty ASPTree with the bounds
     */
    public ASPTree(double minX, double minY, double maxX, double maxY){
        this.root = new ASPNode(minX, minY, maxX, maxY, null);
        // Initialize the BinarySearchTree with the ASP Tree root
        this.centroidTree = new CentroidTree(this.root);
    }
    public boolean put(double x, double y) {
        // Use the CentroidTree to find the smallest ASPNode containing the point
        ASPNode smallestNode = centroidTree.findSmallestBoxContainingPoint(x, y);
        if(smallestNode != null){
            smallestNode.IncrementCounter();
            increaseSize();
            return true;
        }

        return false;
    }
    public ASPNode putAndGetNode(double x, double y) {
        // Call the existing put method
        if (this.root.put(x, y, this)) {
            increaseSize();
            // If put is successful, return the node that contains the point
            return this.root.getNodeContaining(x, y);
        }
        return null;
    }
    /**
     * Estimates the number of points within a search region R.
     * @param R The search region.
     * @return The estimated number of points within the region R.
     */
    public double estimatePointsWithin(Box R) {
        return estimatePointsWithin(this.root, R);
    }
    /**
     * Recursive helper method to estimate the number of points within a region for a given node.
     *
     * @param node The current node in the recursion.
     * @param R The region to search within.
     * @return The estimated number of points within the region at this node.
     */
    private double estimatePointsWithin(ASPNode node, Box R) {
        if (node == null || !node.getBounds().intersects(R)) {
            // If the node is null or the search region does not intersect with the node's bounding box, return 0.
            return 0;
        } else if (node.isHasChildren()) {
            // If the node has children, recursively estimate the count for each child.
            double estimate = 0;
            estimate += estimatePointsWithin(node.getNW(), R);
            estimate += estimatePointsWithin(node.getNE(), R);
            estimate += estimatePointsWithin(node.getSE(), R);
            estimate += estimatePointsWithin(node.getSW(), R);
            return estimate;
        } else {
            // If the node is a leaf, calculate the estimate for this node.
            Box intersection = node.getBounds().intersection(R);
            if (intersection != null) {
                double nodeArea = node.getBounds().area();
                double intersectionArea = intersection.area();
                return node.getCount() * (intersectionArea / nodeArea);
            }
            return 0;
        }
    }
    private void increaseSize() {
        this.size_n++;
    }
    public int getSizeN() {
        return size_n;
    }

    public ASPNode getRoot() {
        return root;
    }
    /**
     * Interface method to get the node containing a point in the ASPTree.
     * @param x The x-coordinate of the point.
     * @param y The y-coordinate of the point.
     * @return The ASPNode containing the point, or null if no such node exists.
     */
    public ASPNode getNodeContaining(double x, double y) {
        if (root == null) {
            return null;
        }
        return root.getNodeContaining(x, y);
    }
    public ASPNode getNodeContaining(Box queryBox) {
        if (root == null) {
            return null;
        }
        return root.getNodeContaining(queryBox);
    }
}

