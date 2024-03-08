package org.example;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private static final double BOX_SIZE = 15.0;
    private static final List<Box> grid = createGrid();
    public static void main(String[] args) {
        String filePath1 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data.txt";
        String filePath2 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data1.txt";
        String filePath3 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data2.txt";
        String filePath4 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data3.txt";
        String filePath5 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data4.txt";
        String filePath7 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data7.txt";
        String filePath8 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data8.txt";
        String filePath9 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data9.txt";
        String filePath10 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data10.txt";
        String filePath11 = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data11.txt";

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

        writeRandomKeywordsOutput();
        String keywordPairsFilePath = "RandomKeywords.txt";
        findRelevantBoxes(keywordPairsFilePath);
    }

    public static void parseFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isCoordinateLine(line)) {
                    String[] parts = line.split("\t", 3);
                    double longitude = Double.parseDouble(parts[0]);
                    double latitude = Double.parseDouble(parts[1]);
                    String text = parts.length > 2 ? parts[2] : "";
                    extractKeywords(text); // This method will now update hashtagFrequency and wordFrequency
                    Point point = new Point(longitude, latitude);
                    processTextToInsertInGrid(point, text);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void extractKeywords(String text) {
        Matcher matcher = hashtagPattern.matcher(text);
        List<String> hashtags = new ArrayList<>();
        List<String> words = new ArrayList<>();

        // Process hashtags for individual frequency and collect them
        while (matcher.find()) {
            String hashtag = matcher.group();
            hashtagFrequency.put(hashtag, hashtagFrequency.getOrDefault(hashtag, 0) + 1);
            hashtags.add(hashtag);
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
        List<String> hashtags = loadKeywords(keywordPairsFilePath); // Update for single hashtags

        // Iterate over hashtags and find relevant boxes
        for (String hashtag : hashtags) {
            Box bestBox = findBoxWithHighestCountForHashtag(hashtag); // Update the method call
            if (bestBox != null) {
                int totalObjects = bestBox.getPoints().size();
                int matchingObjects = calculateMatchingCountForBoxWithHashtag(bestBox, hashtag);

                System.out.println("Best Box for Hashtag: " + hashtag);
                System.out.println("Relevant Box: " + bestBox);
                System.out.println("Number of Objects: " + totalObjects);
                System.out.println("Number of Matching Objects: " + matchingObjects);
                System.out.println("-------------------------");
            } else {
                System.out.println("No relevant box found for hashtag: " + hashtag);
            }
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

        // Placeholder values - Replace with your actual bounds
        double minLongitude = -180.0;
        double maxLongitude = 180.0;
        double minLatitude = -90.0;
        double maxLatitude = 90.0;

        // Calculate grid dimensions to cover the specified area
        int numBoxesX = (int) Math.ceil((maxLongitude - minLongitude) / BOX_SIZE);
        int numBoxesY = (int) Math.ceil((maxLatitude - minLatitude) / BOX_SIZE);

        // Generate the boxes
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



}