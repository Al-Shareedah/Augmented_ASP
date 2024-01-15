package tests;

import org.example.CoordinateParser;
import org.example.RCEstimator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import util.Box;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RCE_test {
    private RCEstimator rcEstimator;
    String queryTerm = "little";
    int actualTermCount = 0;
    @Before
    public void setUp() throws IOException {
        rcEstimator = new RCEstimator(0.01, new Box(-180, -90, 180, 90));
    }

    @Test
    public void testStreamingObjectsInsertion() {
        String filePath = "point_keyword_test.txt";
        int obj_counter = 0;

        // Parse the file and process each line
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (CoordinateParser.isCoordinateLine(line)) {
                    String[] parts = line.split("\t", 3);
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    Set<String> keywords = CoordinateParser.extractKeywords(parts.length > 2 ? parts[2] : "");

                    // Process the point and its keywords in the RCEstimator
                    rcEstimator.processPoint(x, y, keywords);
                    obj_counter++;
                    // Increment the actual term count if the query term is present
                    if (keywords.contains(queryTerm)) {
                        actualTermCount++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to read file: " + e.getMessage());
        }

        // Perform selectivity estimation for the query term
        double estimatedCount = rcEstimator.estimateSelectivity(new Box(-180, -90, 180, 90), Collections.singleton(queryTerm));

        // Log the actual count and the estimated count for comparison
        System.out.println("Actual count of '" + queryTerm + "': " + actualTermCount);
        System.out.println("Estimated count of '" + queryTerm + "': " + estimatedCount);
        System.out.println("Total number of object processed :" + obj_counter);
        // Check if the estimated count is within a reasonable range of the actual count
        assertTrue(Math.abs(estimatedCount - actualTermCount) / actualTermCount < 0.1);
    }
    @Test
    public void testSelectivityEstimate() {
        // Set up the space and RCEstimator with an epsilon threshold
        double epsilon = 0.01;
        Box space = new Box(0, 0, 100, 100);
        RCEstimator rcEstimator = new RCEstimator(epsilon, space);

        // Set up random generator for point and keyword generation
        Random random = new Random();
        int numberOfPoints = 1000; // number of points to insert
        int termToQuery = 3; // the term we will query for
        int actualCount = 0; // to keep track of the actual count of the term

        // Insert random points with randomly selected keywords
        for (int i = 0; i < numberOfPoints; i++) {
            double x = random.nextDouble() * 100;
            double y = random.nextDouble() * 100;
            Set<String> terms = new HashSet<>();
            // Generate a set of 1-6 random keywords for each point
            int termsCount = random.nextInt(6) + 1;
            for (int t = 0; t < termsCount; t++) {
                terms.add(String.valueOf(random.nextInt(6) + 1));
            }
            // If the termToQuery is in the set, increment the actual count
            if (terms.contains(String.valueOf(termToQuery))) {
                actualCount++;
            }
            // Process the point in the RCEstimator
            rcEstimator.processPoint(x, y, terms);
        }

        // Prepare the query
        Box queryRange = new Box(0, 0, 100, 100); // Querying the whole space for simplicity
        Set<String> queryTerms = new HashSet<>();
        queryTerms.add(String.valueOf(termToQuery));

        // Get the selectivity estimate for the term
        double estimatedSelectivity = rcEstimator.estimateSelectivity(queryRange, queryTerms);

        // Assert that the estimated selectivity is close to the actual count
        // You may need to adjust the delta according to the acceptable margin of error
        Assert.assertEquals(actualCount, estimatedSelectivity, actualCount * 0.1);
    }
}
