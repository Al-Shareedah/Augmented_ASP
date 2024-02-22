package tests;
import org.example.ASPNode;
import org.example.ASPTree;
import util.Box;
public class ASPTree_test {
    public static void main(String[] args) {
        // Create an ASPTree instance (adjust bounds as needed)
        ASPTree tree = new ASPTree(-100, -100, 100, 100);

        // Set a smaller alpha value to force more splits
        tree.setAlpha(0.25);

        // Phase 1: Insert points in the SE child to cause splits
        insertClusteredPoints(tree, 5, 5, 5); // Adjust number of points as needed
        System.out.println("****** Tree after Phase 1 ******");
        // Print the tree representation
        double mergeThreshold = (tree.getAlpha() / 2) * tree.getSizeN();
        System.out.println("merge threshold: " + mergeThreshold);
        System.out.println("ASP Tree Structure:");
        printTreeTextFormat(tree.getRoot());

        // Phase 2: Insert points in the NW child
        insertClusteredPoints(tree, -50, -50, 45); // Double the number of points
        System.out.println("\n****** Tree after Phase 2 ******");
        // Print the tree representation
        System.out.println("merge threshold: " + mergeThreshold);
        System.out.println("ASP Tree Structure:");
        printTreeTextFormat(tree.getRoot());



        // Print the merge heap
        System.out.println("\nMerge Heap:");
        tree.printMergeHeap();
    }
    private static void insertClusteredPoints(ASPTree tree, double centerX, double centerY, int numPoints) {
        for (int i = 0; i < numPoints; i++) {
            double x = centerX + Math.random() * 4 - 2; // Offset within a small region
            double y = centerY + Math.random() * 4 - 2;
            tree.putAndGetNode(x, y);
        }
    }

    // Helper method for text-based tree printing
    private static void printTreeTextFormat(ASPNode node) {
        printNode(node, 0, null, false);
    }

    private static void printNode(ASPNode node, int level, ASPNode parent, boolean isRightChild) {
        if (node == null) return;

        // Indentation and lines for previous levels
        for (int i = 0; i < level; i++) {
            if (i == level - 1) {
                System.out.print(parent != null ? (isRightChild ? "├── " : "└── ") : "  ");
            } else {
                System.out.print(parent != null ? "│   " : "    ");
            }
        }

        // Node information
        System.out.println(node.getBounds() + ", count: " + node.getCount() +
                ", key: " + node.getKey() +
                ", mergeable: " + node.isMergeable());

        // Recursively print the children
        printNode(node.getNW(), level + 1, node, false);
        printNode(node.getNE(), level + 1, node, true);
        printNode(node.getSE(), level + 1, node, true);
        printNode(node.getSW(), level + 1, node, false);
    }

}
