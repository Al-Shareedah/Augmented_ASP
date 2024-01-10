package tests;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.example.AASPTree;
import org.example.KMVSynopsis;
import org.example.CoordinateParser;
import org.example.StreamingObject;
import util.Box;

import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.io.BufferedReader;
import java.util.Set;

public class AASPTree_test {
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
                    Set<String> keywords = CoordinateParser.extractKeywords(parts.length > 2 ? parts[2] : "", "english"); // Assume English for simplicity

                    // Create a StreamingObject and add it to the AASPTree
                    StreamingObject obj = new StreamingObject(keywords, x, y);
                    aasptree.update(obj);
                    obj_counter++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to read file: " + e.getMessage());
        }
/*
        // Assert that the KMVSynopsis within AASPTree has processed the objects
        int objectCount = aasptree.kmvSynopsis.getSynopsisSize();
        // Get the normalized hash values
        List<Double> values = aasptree.kmvSynopsis.getNormalizedHashValues();
        // Print all the normalized hash values
        for (double value : values) {
            System.out.println(value +", ");
        }
        System.out.println("The object count in the AASP is: " + objectCount+ "The object passed are: " + obj_counter);
        assertTrue("AASPTree should have processed objects", objectCount > 0);

 */
    }
}

