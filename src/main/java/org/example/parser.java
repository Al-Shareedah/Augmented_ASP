
package org.example;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.example.Point;
public class parser {

    private static final Map<String, Integer> hashtagFrequency = new HashMap<>();
    private static final Map<String, Integer> wordFrequency = new HashMap<>();
    private static final Pattern hashtagPattern = Pattern.compile("#\\w+");
    private static final Set<String> stopWords = new HashSet<>();

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
    }

    public static void parseFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isCoordinateLine(line)) {
                    String[] parts = line.split("\t", 3);
                    String text = parts.length > 2 ? parts[2] : "";
                    extractKeywords(text); // This method will now update hashtagFrequency and wordFrequency
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void extractKeywords(String text) {
        Matcher matcher = hashtagPattern.matcher(text);


        // Process hashtags
        while (matcher.find()) {
            String hashtag = matcher.group();
            hashtagFrequency.put(hashtag, hashtagFrequency.getOrDefault(hashtag, 0) + 1);
        }

        // Split text and filter out stop words and URLs
        String[] words = text.split("[\\s,.!?;:]+");
        for (String word : words) {
            // Check if word is a stop word, starts with "#", or is a URL
            if (!word.isEmpty() && !word.startsWith("#") && !stopWords.contains(word)
                    && !word.contains("//t") && !word.startsWith("https")) {
                wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
            }
        }
    }





    public static boolean isCoordinateLine(String line) {
        Pattern pattern = Pattern.compile("^-?\\d+\\.\\d+\t-?\\d+\\.\\d+.*");
        Matcher matcher = pattern.matcher(line);
        return matcher.find();
    }

    public static void writeRandomKeywordsOutput() {
        // Print the top 10 hashtags
        System.out.println("Top 10 Hashtags:");
        hashtagFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));

        // Print the top 10 words
        System.out.println("\nTop 10 Words:");
        wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(250)
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));

        String outputFilePath = "RandomKeywords.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            List<String> eligibleHashtags = hashtagFrequency.entrySet().stream()
                    .filter(entry -> entry.getValue() > 300)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<String> eligibleWords = wordFrequency.entrySet().stream()
                    .filter(entry -> entry.getValue() > 3000)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            Random rand = new Random();
            int maxLines = Math.min(eligibleHashtags.size(), eligibleWords.size());
            for (int i = 0; i < maxLines; i++) {
                String hashtag = eligibleHashtags.get(rand.nextInt(eligibleHashtags.size()));
                String word = eligibleWords.get(rand.nextInt(eligibleWords.size()));
                writer.write(hashtag + ", " + word + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
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

}
