package org.example;


public class CentroidNode {
    private ASPNode aspNode;
    private int balanceFactor; // Difference in point count between the two partitions
    private CentroidNode leftChild;
    private CentroidNode rightChild;

    public CentroidNode(ASPNode aspNode, int balanceFactor) {
        this.aspNode = aspNode;
        this.balanceFactor = balanceFactor;
    }

    public void setLeftChild(CentroidNode child) {
        this.leftChild = child;
    }

    public void setRightChild(CentroidNode child) {
        this.rightChild = child;
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
}