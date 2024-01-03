package tests;

import org.example.RCEstimator;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import util.Box;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RCE_test {
    @Test
    public void testSelectivityEstimation() throws IOException {
        RCEstimator rcEstimator = new RCEstimator(0.01, new Box(-180, -90, 180, 90));
        HashMap<String, Integer> actualTermCounts = new HashMap<>();
        Random random = new Random();
        String line;
        String queryTerm = "somethin"; // Replace with the actual term to query
        int totalCount = 0;

        Pattern pattern = Pattern.compile("\\s+|,\\s*|\\.\\s*|\\!\\s*|\\?\\s*|\\:\\s*|\\;\\s*|\\@\\s*|\\&\\s*");


        try (BufferedReader reader = new BufferedReader(new FileReader("point_keyword_test.txt"))) {
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                // Check that the line has enough parts
                if (parts.length < 3) {
                    // This line doesn't have enough parts to contain both coordinates and text.
                    continue; // Skip this line and move to the next one.
                }
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);

                // Split the sentence into words using the regex pattern.
                String[] keywords = pattern.split(parts[2]);

                List<String> selectedKeywords = new ArrayList<>();

                // Randomly select keywords for insertion.
                for (String keyword : keywords) {
                    String trimmedKeyword = keyword.trim();
                    if (!trimmedKeyword.isEmpty() && random.nextInt(6) + 1 <= keywords.length) {
                        selectedKeywords.add(trimmedKeyword);
                        actualTermCounts.put(trimmedKeyword, actualTermCounts.getOrDefault(trimmedKeyword, 0) + 1);
                        if (trimmedKeyword.equals(queryTerm)) {
                            totalCount++;
                        }
                    }
                }

                // Process the point and its selected keywords
                rcEstimator.processPoint(x, y, new HashSet<>(selectedKeywords));
            }
        }

        // Perform selectivity estimation for the query term
        double estimatedCount = rcEstimator.estimateSelectivity(new Box(-180, -90, 180, 90), Collections.singleton(queryTerm));

        // Log the actual count and the estimated count for comparison
        System.out.println("Actual count of '" + queryTerm + "': " + totalCount);
        System.out.println("Estimated count of '" + queryTerm + "': " + estimatedCount);

        // Check if the estimated count is within a reasonable range of the actual count
        // This range is arbitrary and might need adjustment based on the estimator's accuracy
        assertTrue(Math.abs(estimatedCount - totalCount) / totalCount < 0.1);
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
