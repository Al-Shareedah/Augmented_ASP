package org.example;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.example.Point;
import util.Box;

public class parser {
    public static class Point {
        public double longitude;
        public double latitude;
        public List<String> keywords; // Add a list to store keywords

        public Point(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.keywords = new ArrayList<>();
        }
    }
    static class KeywordPair {
        String term1;
        String term2;
        public KeywordPair(String term1, String term2) {
            this.term1 = term1;
            this.term2 = term2;
        }
    }

    private static final Map<String, Integer> hashtagFrequency = new HashMap<>();
    private static final Map<String, Integer> wordFrequency = new HashMap<>();
    private static final Map<String, Integer> hashtagPairFrequency = new HashMap<>();
    private static final Map<String, Integer> wordPairFrequency = new HashMap<>();
    private static final Pattern hashtagPattern = Pattern.compile("#\\w+");
    private static final Set<String> stopWords = new HashSet<>();
    // Data structures for new functionality

    private static final double BOX_SIZE = 5.0;
    private static final List<Box> grid = createGrid();
    public static void main(String[] args) {
        String filePath1 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data.txt";
        String filePath2 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data2.txt";
        String filePath3 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data3.txt";
        String filePath4 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data4.txt";
        String filePath5 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data5.txt";
        String filePath7 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data6.txt";
        String filePath8 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data7.txt";
        String filePath9 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data8.txt";
        String filePath10 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data9.txt";
        String filePath11 = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data10.txt";

        String stopWordsFilePath = "C:/Users/fengw/OneDrive/Documents/stopwords.txt"; // Update this to your stop words file path
        loadStopWords(stopWordsFilePath);
        parseFile(filePath1);
        parseFile(filePath2);
        parseFile(filePath3);
        parseFile(filePath4);
        parseFile(filePath5);
        parseFile(filePath7);
        parseFile(filePath8);
        parseFile(filePath9);
        parseFile(filePath10);
        parseFile(filePath11);

        printHighDensityBoxes();
        List<Box> selectedBoxes = selectBoxesWithMoreThan100Points();
        extractAndWriteRandomPoints(selectedBoxes, "workload3.txt");


        writeRandomKeywordsOutput();
        String keywordPairsFilePath = "RandomKeywords.txt";
        findRelevantBoxes(keywordPairsFilePath);

    }

    public static void parseFile(String filePath) {
        try {
            // New approach to read JSON content
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            String[] jsonStrings = content.split("\n");

            ObjectMapper mapper = new ObjectMapper();

            for (String jsonString : jsonStrings) {
                if (jsonString.contains("\"limit\":") || !jsonString.startsWith("{")) {
                    continue;
                }

                JsonNode rootNode = mapper.readTree(jsonString);
                JsonNode coordinatesNode = rootNode.path("coordinates").path("coordinates");

                double longitude, latitude;
                String text = rootNode.path("text").asText("");

                if (!coordinatesNode.isMissingNode() && coordinatesNode.size() > 0) {
                    longitude = coordinatesNode.get(0).asDouble();
                    latitude = coordinatesNode.get(1).asDouble();
                } else {
                    JsonNode boundingBoxNode = rootNode.path("place").path("bounding_box").path("coordinates").get(0);
                    if (boundingBoxNode != null && boundingBoxNode.size() > 0) {
                        double[] centroid = calculateCentroid(boundingBoxNode);
                        longitude = centroid[0];
                        latitude = centroid[1];
                    } else {
                        continue;
                    }
                }

                Point point = new Point(longitude, latitude);
                // Assume extractKeywords(text) updates point.keywords
                extractKeywords(text, point); // Remember to adjust extractKeywords to add keywords to the point if necessary
                insertPointIntoGrid(point);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double[] calculateCentroid(JsonNode polygon) {
        double xSum = 0, ySum = 0;
        int pointCount = polygon.size();

        for (JsonNode point : polygon) {
            double x = point.get(0).asDouble();
            double y = point.get(1).asDouble();
            xSum += x;
            ySum += y;
        }

        return new double[]{xSum / pointCount, ySum / pointCount};
    }
    private static void insertPointIntoGrid(Point point) {
        // Assuming minLongitude = -180, maxLongitude = 180, minLatitude = -90, maxLatitude = 90 for the entire grid
        double minLongitude = -180.0;
        double minLatitude = -90.0;

        // Calculate the indices for the box in which the point should be placed
        int indexX = (int) Math.floor((point.longitude - minLongitude) / BOX_SIZE);
        int indexY = (int) Math.floor((point.latitude - minLatitude) / BOX_SIZE);

        // Calculate the single index based on indexX and indexY if grid is a linear list
        int gridWidth = (int) Math.ceil((360.0) / BOX_SIZE);
        int boxIndex = indexY * gridWidth + indexX;

        // Insert the point into the correct box
        if (boxIndex >= 0 && boxIndex < grid.size()) {
            Box box = grid.get(boxIndex);
            box.getPoints().add(point);
        }
    }

    public static void extractKeywords(String text, parser.Point point) {
        Matcher matcher = hashtagPattern.matcher(text);
        List<String> hashtags = new ArrayList<>();
        List<String> words = new ArrayList<>();

        // Process hashtags for individual frequency and collect them
        while (matcher.find()) {
            String hashtag = matcher.group();
            hashtagFrequency.put(hashtag, hashtagFrequency.getOrDefault(hashtag, 0) + 1);
            hashtags.add(hashtag);
            point.keywords.add(hashtag);
        }

        // Split text and process for individual word frequency (excluding URLs and stop words)
        String[] splitWords = text.split("[\\s,.!?;:]+");
        for (String word : splitWords) {
            if (!word.isEmpty() && !word.startsWith("#") && !stopWords.contains(word.toLowerCase())
                    && !word.contains("//t") && !word.matches("https?://.*")) {
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                words.add(word);
            }
        }
        /*


        // Process hashtag pairs
        for (int i = 0; i < hashtags.size(); i++) {
            for (int j = i + 1; j < hashtags.size(); j++) {
                String first = hashtags.get(i);
                String second = hashtags.get(j);
                if (first.compareTo(second) > 0) {
                    // Swap to ensure lexical order
                    String temp = first;
                    first = second;
                    second = temp;
                }
                String pairKey = first + "," + second;
                hashtagPairFrequency.put(pairKey, hashtagPairFrequency.getOrDefault(pairKey, 0) + 1);
            }
        }

        // Process word pairs, excluding hashtags, URLs, and stop words
        for (int i = 0; i < words.size(); i++) {
            for (int j = i + 1; j < words.size(); j++) {
                String first = words.get(i);
                String second = words.get(j);
                if (first.compareTo(second) > 0) {
                    // Swap to ensure lexical order
                    String temp = first;
                    first = second;
                    second = temp;
                }
                String pairKey = first + "," + second;
                wordPairFrequency.put(pairKey, wordPairFrequency.getOrDefault(pairKey, 0) + 1);
            }
        }

         */
    }





    public static boolean isCoordinateLine(String line) {
        Pattern pattern = Pattern.compile("^-?\\d+\\.\\d+\t-?\\d+\\.\\d+.*");
        Matcher matcher = pattern.matcher(line);
        return matcher.find();
    }

    public static void writeRandomKeywordsOutput() {
        // Print the top 10 Hashtags only
        System.out.println("Top 10 Hashtags:");
        hashtagFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));

        String outputFilePath = "RandomKeywords.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            // Sort eligibleHashtags by their frequency first
            List<String> eligibleHashtags = hashtagFrequency.entrySet().stream()
                    .filter(entry -> entry.getValue() > 100)
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()) // Sort based on frequency
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());


            for (int i = 0; i < 20; i++) {
                if (i < eligibleHashtags.size()) { // Ensure we don't exceed the available count
                    String hashtag = eligibleHashtags.get(i);
                    writer.write(hashtag + "\n"); // Write hashtags in the sorted order
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeKeywordPairsOutput() {
        // Printing top 10 pairs of hashtags to the console
        System.out.println("Top 10 Hashtag Pairs:");
        hashtagPairFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
                .forEach(entry -> {
                    String[] hashtags = entry.getKey().split(",");
                    System.out.println(hashtags[0] + ", " + hashtags[1] + ": " + entry.getValue());
                });
        // Printing top 10 word pairs to the console
        System.out.println("\nTop 10 Word Pairs:");
        wordPairFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    String[] words = entry.getKey().split(",");
                    System.out.println(words[0] + ", " + words[1] + ": " + entry.getValue());
                });

        // Writing top pairs of hashtags to the file
        // Writing top pairs of hashtags to the file
        String outputFilePath = "RandomKeywords.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            hashtagPairFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(15) // Assuming you want the top 10 pairs of hashtags that are frequently found together
                    .forEach(entry -> {
                        try {
                            String[] hashtags = entry.getKey().split(",");
                            writer.write(hashtags[0] + ", " + hashtags[1] + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error writing to " + outputFilePath + ": " + e.getMessage());
        }
    }



    private static void loadStopWords(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error reading stop words file: " + e.getMessage());
        }
    }
    public static void findRelevantBoxes(String keywordPairsFilePath) {
        // Load hashtags with frequency above 50
        List<String> filteredHashtags = hashtagFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() > 50)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        try (PrintWriter writer = new PrintWriter(new FileOutputStream("workload2.txt", true))) { // Append to file
            for (String hashtag : filteredHashtags) {
                Box bestBox = findBoxWithHighestCountForHashtag(hashtag);
                if (bestBox != null) {
                    // Using a set to track unique points based on a string key
                    Set<String> uniquePoints = new HashSet<>();
                    List<Point> uniquePointsWithHashtag = new ArrayList<>();

                    for (Point point : bestBox.getPoints()) {
                        if (point.keywords.contains(hashtag)) {
                            // Create a unique string key for the point
                            String key = point.latitude + "," + point.longitude;
                            // Add to the list only if the key is unique
                            if (uniquePoints.add(key)) {
                                uniquePointsWithHashtag.add(point);
                                if (uniquePointsWithHashtag.size() == 5) {
                                    break; // Stop when five unique points are found
                                }
                            }
                        }
                    }

                    // Write the unique points to the file
                    for (Point point : uniquePointsWithHashtag) {
                        writer.printf("%f\t%f\n", point.latitude, point.longitude);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error writing to workload2.txt: " + e.getMessage());
        }
    }
    private static Box findBoxWithHighestCountForHashtag(String hashtag) {
        Box bestBox = null;
        int maxCount = 0;

        for (Box box : grid) {
            int count = calculateMatchingCountForBoxWithHashtag(box, hashtag);
            if (count > maxCount) {
                bestBox = box;
                maxCount = count;
            }
        }
        return bestBox;
    }

    // New method based on your existing calculateMatchingCountForBox
    private static int calculateMatchingCountForBoxWithHashtag(Box box, String hashtag) {
        int count = 0;

        for (Point point : box.getPoints()) {
            if (point.keywords.contains(hashtag)) {
                count++;
            }
        }

        return count;
    }
    public static void processTextToInsertInGrid(Point point, String text) {
        Box containingBox = findContainingBox(point);
        List<String> keywords = extractKeywordsFromText(text);
        insertPointAndKeywords(containingBox, point, keywords);
    }
    public static void insertPointAndKeywords(Box box, Point point, List<String> keywords) {
        // Add the point to the box's set of points
        box.getPoints().add(point);

        point.keywords.addAll(keywords);
    }

    public static Box findContainingBox(Point point) {
        // Assuming 'grid' is a List<Box> that covers your entire area of interest

        for (Box box : grid) {
            if (box.contains(point.longitude, point.latitude)) {
                return box;
            }
        }

        // If no box is found, you might want to handle this:
        System.err.println("Warning: No containing box found for point: " + point);
        return null;
    }
    private static List<String> loadKeywords(String filePath) {
        List<String> keywords = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                keywords.add(line.trim()); // Add the cleaned-up hashtag
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return keywords;
    }
    private static List<Box> createGrid() {
        List<Box> grid = new ArrayList<>();
        double minLongitude = -180.0;
        double maxLongitude = 180.0;
        double minLatitude = -90.0;
        double maxLatitude = 90.0;
        int numBoxesX = (int) Math.ceil((maxLongitude - minLongitude) / BOX_SIZE);
        int numBoxesY = (int) Math.ceil((maxLatitude - minLatitude) / BOX_SIZE);

        for (int i = 0; i < numBoxesY; i++) {
            double startY = minLatitude + i * BOX_SIZE;
            for (int j = 0; j < numBoxesX; j++) {
                double startX = minLongitude + j * BOX_SIZE;
                Box box = new Box(startX, startY, startX + BOX_SIZE, startY + BOX_SIZE);
                grid.add(box);
            }
        }
        return grid;
    }
    public static List<String> extractKeywordsFromText(String text) {
        List<String> keywords = new ArrayList<>();
        Matcher matcher = hashtagPattern.matcher(text);
        while (matcher.find()) {
            keywords.add(matcher.group());
        }

        String[] words = text.split("[\\s,.!?;:]+");
        for (String word : words) {
            if (!word.isEmpty() && !word.startsWith("#") && !stopWords.contains(word)
                    && !word.contains("//t") && !word.startsWith("https")) {
                keywords.add(word);
            }
        }
        return keywords;
    }
    public static void findRelevantPairsBoxes(String keywordPairsFilePath) {
        List<KeywordPair> keywordPairs = loadKeywordPairs(keywordPairsFilePath);

        // Iterate over keyword pairs and find relevant boxes
        for (KeywordPair pair : keywordPairs) {
            Box bestBox = findBoxWithHighestCount(pair); // Implement this method
            if (bestBox != null) {
                int totalObjects = bestBox.getPoints().size(); // Total objects in the box
                int matchingObjects = calculateMatchingCountForBoxPairs(bestBox, pair);

                System.out.println("Best Box for Keyword Pair: " + pair.term1 + ", " + pair.term2);
                System.out.println("Relevant Box: " + bestBox);
                System.out.println("Number of Objects: " + totalObjects);
                System.out.println("Number of Matching Objects: " + matchingObjects);
                System.out.println("-------------------------");
            } else {
                System.out.println("No relevant box found for keyword pair: " + pair.term1 + ", " + pair.term2);
            }
        }
    }
    private static List<KeywordPair> loadKeywordPairs(String filePath) {
        List<KeywordPair> keywordPairs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] terms = line.trim().split(","); // Assume CSV format
                if (terms.length == 2) {
                    keywordPairs.add(new KeywordPair(terms[0].trim(), terms[1].trim()));
                } else {
                    // Handle lines that don't have two keywords
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return keywordPairs;
    }

    private static Box findBoxWithHighestCount(KeywordPair pair) {
        Box bestBox = null;
        int maxCount = 0;

        for (Box box : grid) {
            int count = calculateMatchingCountForBoxPairs(box, pair);

            if (count > maxCount) {
                bestBox = box;
                maxCount = count;
            }
        }
        return bestBox;
    }

    private static int calculateMatchingCountForBoxPairs(Box box, KeywordPair pair) {
        int count = 0;

        for (Point point : box.getPoints()) {
            if (point.keywords.contains(pair.term1) || point.keywords.contains(pair.term2)) {
                count++;
            }
        }

        return count;
    }
    public static void printHighDensityBoxes() {
        int count = 0;
        for (Box box : grid) {
            if (box.getPoints().size() > 100) {
                System.out.println("Box: " + box + " has " + box.getPoints().size() + " points.");
                count++;
            }
        }
        System.out.println("Total boxes with more than 100 points: " + count);
    }

    private static List<Box> selectBoxesWithMoreThan100Points() {
        List<Box> selectedBoxes = new ArrayList<>();
        for (Box box : grid) {
            if (box.getPoints().size() > 100) {
                selectedBoxes.add(box);
            }
        }
        return selectedBoxes;
    }

    private static void extractAndWriteRandomPoints(List<Box> boxes, String filename) {
        Set<String> writtenPoints = new HashSet<>();
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(filename))) {
            for (Box box : boxes) {
                List<Point> points = new ArrayList<>(box.getPoints());
                Collections.shuffle(points); // Randomly shuffle the list of points
                int pointsToWrite = Math.min(20, points.size()); // Select 20 or fewer points
                for (Point point : points) {
                    // Create a unique key for each point
                    String pointKey = point.latitude + "," + point.longitude;
                    // Check if the point is already written, if not, write to the file
                    if (!writtenPoints.contains(pointKey)) {
                        writer.printf("%f\t%f\n", point.latitude, point.longitude);
                        writtenPoints.add(pointKey); // Mark this point as written
                        if (--pointsToWrite == 0) break; // Decrement pointsToWrite and check if we are done
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}