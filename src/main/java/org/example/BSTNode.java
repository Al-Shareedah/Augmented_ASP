package org.example;
import util.Box;
public class BSTNode {
    BSTNode left, right;
    // Sizes of the left and right ASP subtrees
    int leftChildSize, rightChildSize;
    // Reference to the root of the corresponding subtree in the ASP tree
    ASPNode aspSubtreeRoot;

    public BSTNode(ASPNode aspSubtreeRoot) {
        this.aspSubtreeRoot = aspSubtreeRoot;
        this.left = null;
        this.right = null;

        this.leftChildSize = 0;
        this.rightChildSize = 0;
    }


    public void updateChildSizes() {
        if (this.left != null && this.left.aspSubtreeRoot != null) {
            this.leftChildSize = this.left.aspSubtreeRoot.subtreeSize();
        }
        if (this.right != null && this.right.aspSubtreeRoot != null) {
            this.rightChildSize = this.right.aspSubtreeRoot.subtreeSize();
        }
    }
}
