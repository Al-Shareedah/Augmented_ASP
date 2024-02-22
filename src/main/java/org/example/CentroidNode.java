package org.example;


public class CentroidNode {
    private ASPNode aspNode;
    private int balanceFactor; // Difference in point count between the two partitions
    private CentroidNode leftChild;
    private CentroidNode rightChild;
    private CentroidNode parent; // Add parent reference
    private int subtreeSize; // Size of the subtree rooted at this node

    public CentroidNode(ASPNode aspNode, int balanceFactor) {
        this.aspNode = aspNode;
        this.balanceFactor = balanceFactor;
        this.leftChild = null;
        this.rightChild = null;
        this.parent = null; // Initialize parent as null
    }

    public void setLeftChild(CentroidNode child) {
        this.leftChild = child;
        if (child != null) {
            child.parent = this; // Set this node as parent of the child
        }
    }

    public void setRightChild(CentroidNode child) {
        this.rightChild = child;
        if (child != null) {
            child.parent = this; // Set this node as parent of the child
        }
    }

    // Getter and Setter for the parent
    public CentroidNode getParent() {
        return parent;
    }

    public void setParent(CentroidNode parent) {
        this.parent = parent;
    }

    public int getPointCount() {
        return aspNode.getCount();
    }

    public CentroidNode getLeftChild() {
        return leftChild;
    }

    public CentroidNode getRightChild() {
        return rightChild;
    }

    public ASPNode getAspNode() {
        return aspNode;
    }
    // Method to set the size of the subtree rooted at this node
    public void setSubtreeSize(int size) {
        this.subtreeSize = size;
    }

    // Method to get the size of the subtree rooted at this node
    public int getSubtreeSize() {
        return this.subtreeSize;
    }
}