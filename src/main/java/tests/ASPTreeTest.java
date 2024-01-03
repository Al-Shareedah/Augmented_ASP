package tests;

import org.example.ASPNode;
import org.example.ASPTree;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import util.Box;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class ASPTreeTest {
    @Test
    public void testMinimumResolution() {
        // Create an ASPTree with a specific boundary
        ASPTree tree = new ASPTree(0, 0, 10, 10);

        // Insert points in a pattern to force subdivision in a specific region
        for (double i = 0; i <= 1; i += 0.1) {
            for (double j = 0; j <= 1; j += 0.1) {
                tree.put(i, j);
            }
        }

        // Check if any leaf node is smaller than the minimum resolution
        assertFalse(isSubdividedBeyondMinimum(tree.getRoot()));
    }

    private boolean isSubdividedBeyondMinimum(ASPNode node) {
        if (node.isHasChildren()) {
            // Recursively check each child
            return isSubdividedBeyondMinimum(node.getNW()) ||
                    isSubdividedBeyondMinimum(node.getNE()) ||
                    isSubdividedBeyondMinimum(node.getSE()) ||
                    isSubdividedBeyondMinimum(node.getSW());
        } else {
            // For a leaf node, check its boundary size
            Box bounds = node.getBounds();
            return (bounds.maxX - bounds.minX) < 1.0 || (bounds.maxY - bounds.minY) < 1.0;
        }
    }

    @Test
    public void testEstimatePointsWithin() {
        // Create an ASPTree with a specific boundary
        ASPTree tree = new ASPTree(0, 0, 10, 10);

        // Insert points into the ASPTree
        for (double i = 0; i <= 10; i += 1) {
            for (double j = 0; j <= 10; j += 1) {
                tree.put(i, j);
            }
        }

        // Define a search region R within the boundary of the tree
        Box searchRegion = new Box(2, 2, 5, 5);

        // Estimate the number of points within the search region
        double estimatedPoints = tree.estimatePointsWithin(searchRegion);

        // Calculate expected number of points within the search region
        // In this case, it should be the actual number of points since we are using a grid pattern
        int actualPoints = 0;
        for (double i = 2; i < 5; i += 1) {
            for (double j = 2; j < 5; j += 1) {
                if (searchRegion.contains(i, j)) {
                    actualPoints++;
                }
            }
        }

        // Assert that the estimated number of points is equal to the actual number of points
        // Note: Depending on the implementation, you might want to allow for a small error margin
        assertEquals(actualPoints, estimatedPoints, "The estimated number of points within the search region should be equal to the actual number of points.");
    }
    @Test
    public void testEstimatePointsWithinLargeArea() {
        // Create an ASPTree with a larger boundary
        ASPTree tree = new ASPTree(0, 0, 100, 100);

        // Insert a larger number of points into the ASPTree
        for (double i = 0; i <= 100; i += 0.5) { // Adjust the increment if needed
            for (double j = 0; j <= 100; j += 0.5) { // Adjust the increment if needed
                tree.put(i, j);
            }
        }

        // Define a larger search region R within the boundary of the tree
        Box searchRegion = new Box(10, 10, 50, 50);

        // Estimate the number of points within the search region
        double estimatedPoints = tree.estimatePointsWithin(searchRegion);

        // Calculate the expected number of points within the search region
        // The grid pattern may not perfectly align with the search region, but we'll calculate the exact count
        int actualPoints = 0;
        for (double i = 10; i < 50; i += 0.5) { // Adjust the loop to match the inserted points
            for (double j = 10; j < 50; j += 0.5) { // Adjust the loop to match the inserted points
                if (searchRegion.contains(i, j)) {
                    actualPoints++;
                }
            }
        }

        // Assert that the estimated number of points is close to the actual number of points
        // Allow for a small error margin due to floating-point arithmetic precision
        double errorMargin = actualPoints * 0.01; // 1% error margin, adjust as appropriate
        assertEquals(actualPoints, estimatedPoints, errorMargin, "The estimated number of points within the search region should be close to the actual number of points.");
    }



}
