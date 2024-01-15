package tests;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.example.AASPTree;
import org.example.CoordinateParser;
import org.example.StreamingObject;
import util.Box;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.io.BufferedReader;

public class AASPTree_test {
    private AASPTree aasptree;
    private static final double DATA_SIZE = 1000;

    @Before
    public void setUp() throws NoSuchAlgorithmException {
        // Initialize AASPTree with the given data size
        aasptree = new AASPTree(DATA_SIZE, new Box(-180, -90, 180, 90));
        // Define the query box and keywords

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
            System.out.printf("Done");
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to read file: " + e.getMessage());
        }

        // Assert that the KMVSynopsis within AASPTree has processed the objects
        int objectCount = aasptree.getSynopsisSize();


        Box queryBox = new Box(-97.31554113, -31.399994, 115.22774061, 56.05831074);
        Set<String> queryKeywords = new HashSet<>(Arrays.asList("the", "I", "love"));
        aasptree.estimateSelectivity(queryBox, queryKeywords, 100);


        System.out.println("The object count in the AASP is: " + objectCount+ "The object passed are: " + obj_counter);
        assertTrue("AASPTree should have processed objects", objectCount > 0);


    }
    @Test
    public void testLocalBoosting() throws Exception {
        // Initialize AASPTree with a large enough space
        AASPTree tree = new AASPTree(2000, new Box(-100, -100, 100, 100));
        int actualCount = 0;  // Counter for both keywords within query region
        int partialCount = 0; // Counter for at least one keyword within query region
        Set<String> queryKeywords = new HashSet<>(Arrays.asList("keyword1", "keyword2"));
        // Create an instance of Random here
        Random random = new Random();
        for (int i = 0; i < 2000; i++) {
            double x, y;

            // Randomly decide to which box this point belongs
            if (random.nextBoolean()) {
                // Box: minX = -5, minY = -5, maxX = 5, maxY = 5
                x = -5 + 10 * random.nextDouble(); // Random x between -5 and 5
                y = -5 + 10 * random.nextDouble(); // Random y between -5 and 5
            } else {
                // Box: minX = -20, minY = -20, maxX = 20, maxY = 20
                x = -10 + 20 * random.nextDouble(); // Random x between -20 and 20
                y = -10 + 20 * random.nextDouble(); // Random y between -20 and 20
            }

            // Generate two random keywords
            String keyword1 = "keyword" + random.nextInt(5);
            String keyword2 = "keyword" + random.nextInt(5);

            // Create a set with both keywords
            HashSet<String> keywords = new HashSet<>(Arrays.asList(keyword1, keyword2));

            // Add the object with these keywords to the tree
            tree.update(new StreamingObject(keywords, x, y));
            // Check if the point is within the query region
            boolean isInQueryRegion = x >= -5 && x <= -1 && y >= -5 && y <= -1;

            // Check if both query keywords are present
            if (keywords.containsAll(queryKeywords) && isInQueryRegion) {
                actualCount++;
            }

            // Check if at least one query keyword is present
            if (!Collections.disjoint(keywords, queryKeywords) && isInQueryRegion) {
                partialCount++;
            }
        }

        // Define a small query box
        Box queryBox = new Box(-5, -5, -1, -1);


        // Determine initial representative sample count (K) for the query box
        int initialK = tree.countRepresentativeSamples(tree.getObjSamples(queryBox), new HashSet<>(Arrays.asList("keyword1", "keyword2")));

        // Set a K threshold (for example, slightly higher than initialK to test boosting)
        int K_threshold = initialK + 1;
        // Perform selectivity estimation which internally calls local boosting if needed
        double estimatedValue = tree.estimateSelectivity(queryBox, queryKeywords, K_threshold);
        System.out.println("The actual count of the query is: " + actualCount);
        double marginOfError = 0.50; // 50% margin of error
        double lowerBound = actualCount * (1 - marginOfError);
        double upperBound = actualCount * (1 + marginOfError);

        assertTrue("Estimated value should be within a high margin of error of the actual count",
                estimatedValue >= lowerBound && estimatedValue <= upperBound);

    }
}

