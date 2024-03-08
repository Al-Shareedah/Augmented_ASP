package org.example;
import util.Box;

import java.util.*;

public class ASPTree {
    protected ASPNode root = null;

    private double alpha = 0.5;
    private int size_n = 0;
    // Initialize the min-heap M
    private PriorityQueue<ASPNode> mergeHeap = new PriorityQueue<>((node1, node2) -> Integer.compare(node1.getKey(), node2.getKey()));

    /**
     * Creates an empty ASPTree with the bounds
     */
    public ASPTree(double minX, double minY, double maxX, double maxY){
        this.root = new ASPNode(minX, minY, maxX, maxY, null);

    }
    public boolean put(double x, double y) {
        // Find the smallest node containing the point
        ASPNode smallestNode = this.root.getSmallestNodeContaining(x, y);
        if (smallestNode != null) {
            smallestNode.IncrementCounter();
            updateMergeHeap(smallestNode);
            increaseSize();

            // Check if the counter of node v is larger than the split threshold
            if (smallestNode.getCount() > alpha * size_n) {
                smallestNode.refine(); // perform refinement

                updateMergeHeapAfterRefinement(smallestNode); // Update the heap after refinement
            }
            checkAndMergeIfRequired();
            return true;
        }
        return false;
    }
    public ASPNode putAndGetNode(double x, double y) {
        // Find the smallest node containing the point
        ASPNode smallestNode = this.root.getSmallestNodeContaining(x, y);
        if (smallestNode != null) {
            smallestNode.IncrementCounter();
            updateMergeHeap(smallestNode);
            increaseSize();

            // Check if the counter of node v is larger than the split threshold
            if (smallestNode.getCount() > alpha * size_n) {
                smallestNode.refine(); // perform refinement

                updateMergeHeapAfterRefinement(smallestNode); // Update the heap after refinement
            }
            checkAndMergeIfRequired();

            // Return smallestNode for use in putAndGetNode
            return smallestNode;
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
        // Base case: if the node is null, return 0
        if (node == null) {
            return 0;
        }

        // Calculate the intersection of the node's bounding box with the region
        Box intersection = node.getBounds().intersection(R);
        if (intersection == null) {
            // No intersection, so this node does not contribute to the sum
            return 0;
        }

        double intersectionArea = intersection.area();
        double nodeArea = node.getBounds().area();

        // Calculate the current node's contribution to the estimate
        double currentNodeEstimate = node.getCount() * (intersectionArea / nodeArea);

        // Recursively calculate the estimates for child nodes and sum them up
        double childrenEstimate = 0;
        if (node.isHasChildren()) {
            childrenEstimate += estimatePointsWithin(node.getNW(), R);
            childrenEstimate += estimatePointsWithin(node.getNE(), R);
            childrenEstimate += estimatePointsWithin(node.getSE(), R);
            childrenEstimate += estimatePointsWithin(node.getSW(), R);
        }

        // Return the sum of the current node's estimate and the children's estimates
        return currentNodeEstimate + childrenEstimate;
    }

    private void checkAndMergeIfRequired() {
        if (!mergeHeap.isEmpty()) {
            ASPNode headNode = mergeHeap.peek(); // Get the head of the heap without removing it
            int keyV = headNode.getKey(); // Assuming getKey() method is implemented to compute key(v)

            if (keyV < (alpha * size_n)/2) {
                // Perform the merge operation
                mergeChildrenIntoNode(headNode);
                // After merging, update the heap accordingly
                mergeHeap.remove(headNode); // Remove the old headNode state from the heap

                //check if v is mergable and its parent as well
                if (headNode.isMergeable()) { // Check if it's still mergeable after merge
                    mergeHeap.add(headNode); // Re-add to reflect the new key value
                }
                // Reassess the parent node of the merged node
                ASPNode parentNode = headNode.getParent();
                if (parentNode != null) {
                    // If the parent node is already in the heap, it needs to be reassessed and potentially updated
                    if (mergeHeap.contains(parentNode)) {
                        mergeHeap.remove(parentNode); // Remove to reassess
                        if (parentNode.isMergeable()) { // Check if it still meets the criteria
                            mergeHeap.add(parentNode); // Re-add it to the heap with its updated key
                        }
                        // If the parent node was not in the heap but now meets the criteria, it should also be added
                    } else if (parentNode.isMergeable()) {
                        mergeHeap.add(parentNode); // Directly add it if it now meets the criteria
                    }
                }
            }
        }
    }
    private void mergeChildrenIntoNode(ASPNode node) {
        // Sum the counters of node's children and update node's counter
        int childrenSum = 0;
        if (node.getNW() != null) childrenSum += sumChildrenCounts(node.getNW());
        if (node.getNE() != null) childrenSum += sumChildrenCounts(node.getNE());
        if (node.getSE() != null) childrenSum += sumChildrenCounts(node.getSE());
        if (node.getSW() != null) childrenSum += sumChildrenCounts(node.getSW());
        node.setCount(node.getCount() + childrenSum); // Update count(v) with the sum of its children's counts

        // Temporarily store grandchildren to potentially make them direct children of 'node'
        List<ASPNode> potentialNewChildren = new ArrayList<>();

        // Process each child of 'node' to handle grandchildren
        addGrandChildren(node.getNW(), potentialNewChildren);
        addGrandChildren(node.getNE(), potentialNewChildren);
        addGrandChildren(node.getSE(), potentialNewChildren);
        addGrandChildren(node.getSW(), potentialNewChildren);

        // Reset 'node's children to null as they are being merged
        node.setNW(null);
        node.setNE(null);
        node.setSE(null);
        node.setSW(null);

        // Restrict to only four grandchildren due to the stated condition
        int count = 0;
        for (ASPNode grandchild : potentialNewChildren) {
            if (count >= 4) break; // Ensure no more than four grandchildren are adopted

            // Assign grandchild to 'node', ensuring it points back to 'node' as its parent
            assignGrandchildToNode(node, grandchild, count);
            count++;
        }

        // Update node to indicate it may now have children if any grandchildren were adopted
        if (!potentialNewChildren.isEmpty()) {
            node.setHasChildren(true);
        }
    }
    // Helper method to sum the counts of a node and its descendants
    private int sumChildrenCounts(ASPNode node) {
        if (node == null) return 0;
        int sum = node.getCount(); // Start with the node's own count
        // Recursively add the counts of the node's children
        sum += sumChildrenCounts(node.getNW());
        sum += sumChildrenCounts(node.getNE());
        sum += sumChildrenCounts(node.getSE());
        sum += sumChildrenCounts(node.getSW());
        return sum;
    }
    // Helper method to collect grandchildren nodes
    private void addGrandChildren(ASPNode child, List<ASPNode> potentialNewChildren) {
        if (child == null) return;
        if (child.getNW() != null) potentialNewChildren.add(child.getNW());
        if (child.getNE() != null) potentialNewChildren.add(child.getNE());
        if (child.getSE() != null) potentialNewChildren.add(child.getSE());
        if (child.getSW() != null) potentialNewChildren.add(child.getSW());
    }
    // Helper method to assign a grandchild to 'node', managing which position it should take
    private void assignGrandchildToNode(ASPNode node, ASPNode grandchild, int position) {
        grandchild.setParent(node); // Set 'node' as the parent of the grandchild
        switch (position) {
            case 0:
                node.setNW(grandchild);
                break;
            case 1:
                node.setNE(grandchild);
                break;
            case 2:
                node.setSE(grandchild);
                break;
            case 3:
                node.setSW(grandchild);
                break;
        }
    }

    // Method to update the heap after an insert or delete
    private void updateMergeHeap(ASPNode node) {
        // Update the node itself if it's mergeable
        if (node.isMergeable()) {
            if (mergeHeap.contains(node)) {
                mergeHeap.remove(node); // Remove and re-add to update its position in the heap
            }
            mergeHeap.add(node);
        }

        // Update the parent node if it's mergeable
        ASPNode parentNode = node.getParent();
        if (parentNode != null && parentNode.isMergeable()) {
            if (mergeHeap.contains(parentNode)) {
                mergeHeap.remove(parentNode); // Remove and re-add to update its position in the heap
            }
            mergeHeap.add(parentNode);
        }
    }
    private void updateMergeHeapAfterRefinement(ASPNode node) {
        // Since the node was just refined, it automatically becomes mergeable
        // Add it directly to the merge heap
        mergeHeap.add(node);


        // Check if the parent of the node has more than two non-leaf children
        ASPNode parentNode = node.getParent();
        if (parentNode != null && hasMoreThanTwoNonLeafChildren(parentNode)) {
            // Remove the parent from the merge heap if it's present
            mergeHeap.remove(parentNode);
        }

        // Additionally, consider if the parent itself becomes mergeable after changes
        if (parentNode != null && parentNode.isMergeable() && !mergeHeap.contains(parentNode)) {
            mergeHeap.add(parentNode);
        }
    }
    // Helper method to determine if a node has more than two non-leaf children
    private boolean hasMoreThanTwoNonLeafChildren(ASPNode node) {
        int nonLeafChildrenCount = 0;
        if (node.getNW() != null && node.getNW().isHasChildren()) nonLeafChildrenCount++;
        if (node.getNE() != null && node.getNE().isHasChildren()) nonLeafChildrenCount++;
        if (node.getSE() != null && node.getSE().isHasChildren()) nonLeafChildrenCount++;
        if (node.getSW() != null && node.getSW().isHasChildren()) nonLeafChildrenCount++;

        return nonLeafChildrenCount > 2;
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


    public void printMergeHeap() {
        PriorityQueue<ASPNode> heapCopy = new PriorityQueue<>(mergeHeap);
        System.out.println("Merge Heap (in order of priority):");
        while (!heapCopy.isEmpty()) {
            ASPNode node = heapCopy.poll();
            System.out.print(node.getCount() + " "); // Adjust this line to print the desired node attribute
        }
        System.out.println();
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getAlpha() {
        return alpha;
    }
}

