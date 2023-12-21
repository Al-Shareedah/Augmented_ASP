package org.example;
import util.Box;
public class BinarySearchTree {
    private BSTNode root;

    public BinarySearchTree() {
        root = null;
    }

    public void insert(ASPNode aspSubtreeRoot) {
        root = insertRec(root, aspSubtreeRoot);
    }

    private BSTNode insertRec(BSTNode root, ASPNode aspSubtreeRoot) {
        if (root == null) {
            return new BSTNode(aspSubtreeRoot);
        }
        Box currentBox = root.aspSubtreeRoot.getBounds();
        Box newBox = aspSubtreeRoot.getBounds();
        if (Box.compareBoxes(newBox, currentBox) > 0) {
            // If new box is larger, insert to the left
            root.left = insertRec(root.left, aspSubtreeRoot);
        } else {
            // If new box is smaller or equal, insert to the right
            root.right = insertRec(root.right, aspSubtreeRoot);
        }

        // Update the sizes of the left and right children after insertion
        root.updateChildSizes();

        return root;
    }


    public ASPNode findSmallestBox(double x, double y) {
        return findSmallestBoxRec(root, x, y, null);
    }

    private ASPNode findSmallestBoxRec(BSTNode node, double x, double y, ASPNode smallestSoFar) {
        if (node == null) {
            return smallestSoFar;
        }

        Box currentBox = node.aspSubtreeRoot.getBounds();
        if (currentBox.contains(x, y)) {
            if (smallestSoFar == null || Box.isSmaller(currentBox, smallestSoFar.getBounds())) {
                smallestSoFar = node.aspSubtreeRoot;
            }
        }

        // Traverse left and right subtrees
        smallestSoFar = findSmallestBoxRec(node.left, x, y, smallestSoFar);
        smallestSoFar = findSmallestBoxRec(node.right, x, y, smallestSoFar);

        return smallestSoFar;
    }

}
