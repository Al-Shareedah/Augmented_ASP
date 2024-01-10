package tests;
import org.example.*;
import org.junit.Before;
import org.junit.Test;

import util.Box;
import org.example.CoordinateParser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class KMVSynopsisTest {
    @Test
    public void testQueryKeywordTony() {
        // Initialize the KMVSynopsis
        KMVSynopsis kmvSynopsis = new KMVSynopsis(1000); // Data size is arbitrary for the test

        // Create StreamingObject instances
        StreamingObject obj1 = new StreamingObject(new HashSet<>(Arrays.asList("disappoint", "constantly", "male", "population")), -0.76176752, 51.24830719);
        StreamingObject obj2 = new StreamingObject(new HashSet<>(Arrays.asList("fund", "want", "have", "to", "just")), -47.85229446, -15.63417584);
        StreamingObject obj3 = new StreamingObject(new HashSet<>(Arrays.asList("surprise", "Big", "pleasantly", "be", "Scotch", "Sound", "Ale", "drink")), -104.995, 39.7424);
        StreamingObject obj4 = new StreamingObject(new HashSet<>(Arrays.asList("thanks", "Proud", "Tony", "North", "do", "good", "East")), -118.48101858, 33.99647333);
        StreamingObject obj5 = new StreamingObject(new HashSet<>(Arrays.asList("PA", "overcast", "Tony", "Airport", "Pittsburgh", "International")), -80.24648743, 40.49546389);

        // Insert objects into KMVSynopsis
        kmvSynopsis.updateKMVSynopses(obj3);
        kmvSynopsis.updateKMVSynopses(obj2);
        kmvSynopsis.updateKMVSynopses(obj4);
        kmvSynopsis.updateKMVSynopses(obj5);
        kmvSynopsis.updateKMVSynopses(obj1);

        // Debugging: Print all objects in KMVSynopsis
       // printAllObjectsInKMVSynopsis(kmvSynopsis);
        // Query the keyword "Tony"
        Set<StreamingObject> result = kmvSynopsis.queryByKeyword("Tony");

        // Check that the result contains the correct objects
        assertTrue(result.contains(obj4), "The result should contain the object with keyword 'Tony'");
        assertTrue(result.contains(obj5), "The result should contain the object with keyword 'Tony'");
    }
    // Method to print all objects in KMVSynopsis for debugging
    private void printAllObjectsInKMVSynopsis(KMVSynopsis kmvSynopsis) {
        for (Map.Entry<String, Synopsis> entry : kmvSynopsis.getTermSynopses().entrySet()) {
            String term = entry.getKey();
            Synopsis synopsis = entry.getValue();
            System.out.println("Term: " + term + ", Objects:");
            for (Map.Entry<Long, SortedSet<StreamingObject>> gridEntry : synopsis.getGridIndex().entrySet()) {
                for (StreamingObject obj : gridEntry.getValue()) {
                    System.out.println("    Object with keywords: " + obj.getAssociatedTerms());
                }
            }
        }
    }
    private AASPTree aasptree;
    private static final double DATA_SIZE = 1000;
    @Before
    public void setUp() throws NoSuchAlgorithmException {
        // Initialize AASPTree with the given data size
        aasptree = new AASPTree(DATA_SIZE, new Box(-180, -90, 180, 90));

        // Initialize NLP pipelines (Assuming this is a static method in CoordinateParser)
        CoordinateParser.initPipelines();
    }
    @Test
    public void testEstimateSelectivity() {
        KMVSynopsis kmvSynopsis = new KMVSynopsis(DATA_SIZE); // Assuming 1000 is the data size
        // Assuming AASPTree is similar to KMVSynopsis for this context
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
                    Set<String> keywords = CoordinateParser.extractKeywords(parts.length > 2 ? parts[2] : "", "english");

                    // Create a StreamingObject and add it to the KMVSynopsis
                    StreamingObject obj = new StreamingObject(keywords, x, y);
                    kmvSynopsis.updateKMVSynopses(obj);
                    obj_counter++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to read file: " + e.getMessage());
        }


        Box queryBox = new Box(-180, -90, 180, 90);

        // Define the query keywords set
        Set<String> queryKeywords = new HashSet<>(Arrays.asList("sozinha", "pensar", "Home"));

        // Estimate the selectivity
        int representativeSamples = kmvSynopsis.getRepSamples(queryKeywords, queryBox);

        // Assert the number of representative samples. The expected number needs to be determined based on actual data.
        // This is a placeholder value and should be replaced with the correct expected value.
        int expectedNumberOfRepresentativeSamples = 3; // Placeholder: this should be calculated or known beforehand.
        assertEquals(expectedNumberOfRepresentativeSamples, representativeSamples);
    }
}
