
package org.example;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.example.Point;
public class parser {

    private static final Set<String> allKeywords = new HashSet<>();

    public static void main(String[] args) {
        String filePath = "point_keyword_test.txt";
        parseFile(filePath);
        writeRandomKeywordsOutput();
    }

    public static void parseFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isCoordinateLine(line)) {
                    String[] parts = line.split("\t", 3);
                    String text = parts.length > 2 ? parts[2] : "";
                    allKeywords.addAll(extractKeywords(text));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        String[] words = text.split("[\\s,.!?;:]+");
        Collections.addAll(keywords, words);
        return keywords;
    }

    public static boolean isCoordinateLine(String line) {
        Pattern pattern = Pattern.compile("^-?\\d+\\.\\d+\t-?\\d+\\.\\d+.*");
        Matcher matcher = pattern.matcher(line);
        return matcher.find();
    }

    public static void writeRandomKeywordsOutput() {
        String outputFilePath = "RandomKeywords.txt"; // Specify your output file path here
        Random rand = new Random();
        String[] allKeywordsArray = allKeywords.toArray(new String[0]);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            // Assuming the output should be as many lines as keywords
            for (int i = 0; i < allKeywords.size(); i++) {
                String line = getRandomKeywords(rand, 2, 3, allKeywordsArray) + "\n";
                writer.write(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getRandomKeywords(Random rand, int min, int max, String[] allKeywordsArray) {
        int count = rand.nextInt(max - min + 1) + min;
        Set<String> selectedKeywords = new HashSet<>();
        while (selectedKeywords.size() < count) {
            selectedKeywords.add(allKeywordsArray[rand.nextInt(allKeywordsArray.length)]);
        }
        return String.join(", ", selectedKeywords);
    }

}
