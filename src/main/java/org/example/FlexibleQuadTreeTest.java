package org.example;
import org.example.FlexibleQuadTree;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
public class FlexibleQuadTreeTest {

    private static final int SUB_INS = 10000;
    private static final Object TOKEN = "";

    public static void main(String[] args) {
        final SpatialIndex<Object> qt = new FlexibleQuadTree<>();
        // Insert points into the quadtree
        IntStream.range(0, SUB_INS).forEach(v -> {
            final double val = v / (double) SUB_INS;
            qt.insert(v, new double[] {val, val});
            qt.insert(v, new double[] {-val, val});
            qt.insert(v, new double[] {val, -val});
            qt.insert(v, new double[] {-val, -val});
        });

        // Perform queries
        System.out.println("Querying the entire range");
        System.out.println("Expected size: " + 4 * SUB_INS + ", Actual size: " +
                qt.query(new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE}, new double[] {Double.MAX_VALUE, Double.MAX_VALUE}).size());

        // Moving points
        System.out.println("Moving points to new locations");
        IntStream.range(0, SUB_INS).forEach(v -> {
            final double val = v / (double) SUB_INS / 2;
            qt.move(v, new double[] {val, val}, new double[] {val / 2, val / 2});
            qt.move(v, new double[] {-val, val}, new double[] {-val / 2, val / 2});
            qt.move(v, new double[] {val, -val}, new double[] {val / 2, -val / 2});
            qt.move(v, new double[] {-val, -val}, new double[] {-val / 2, -val / 2});
        });

        // Removing points
        System.out.println("Removing points");
        IntStream.range(0, SUB_INS).forEach(v -> {
            final double val = v / (double) SUB_INS / 2;
            qt.remove(v, new double[] {val, val});
            qt.remove(v, new double[] {-val, val});
            qt.remove(v, new double[] {val, -val});
            qt.remove(v, new double[] {-val, -val});
        });

        // Verifying the tree is empty
        System.out.println("Verifying the tree is empty");
        System.out.println("Expected size: 0, Actual size: " +
                qt.query(new double[] {-Double.MAX_VALUE, -Double.MAX_VALUE}, new double[] {Double.MAX_VALUE, Double.MAX_VALUE}).size());

    }

 
}