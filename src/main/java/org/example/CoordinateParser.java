package org.example;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class CoordinateParser {

    private static StanfordCoreNLP englishPipeline;


    public static void main(String[] args) {
        // Initialize NLP pipelines
        initPipelines();

        String filePath = "point_keyword_test.txt";
        parseFile(filePath);
    }
    public static void initPipelines() {
        Properties englishProps = new Properties();
        englishProps.setProperty("annotators", "tokenize,ssplit");
        englishPipeline = new StanfordCoreNLP(englishProps);


    }
    public static void parseFile(String filePath) {

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            double x = 0, y = 0;
            StringBuilder textBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (isCoordinateLine(line)) {
                    if (textBuilder.length() > 0) {
                        Set<String> keywords = extractKeywords(textBuilder.toString(), "english"); // Change language as needed
                        System.out.println("Coordinates: (" + x + ", " + y + "), Keywords: " + keywords);
                        textBuilder.setLength(0);
                    }

                    String[] parts = line.split("\t", 3);
                    x = Double.parseDouble(parts[0]);
                    y = Double.parseDouble(parts[1]);
                    textBuilder.append(parts.length > 2 ? parts[2] : "");
                } else {
                    textBuilder.append(line).append("\n");
                }
            }

            if (textBuilder.length() > 0) {
                Set<String> keywords = extractKeywords(textBuilder.toString(), "english"); // Change language as needed
                System.out.println("Coordinates: (" + x + ", " + y + "), Keywords: " + keywords);

            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static Set<String> extractKeywords(String text, String language) {
        Set<String> keywords = new HashSet<>();
        Annotation document = new Annotation(text);
        StanfordCoreNLP pipeline = englishPipeline;  // Always use English pipeline

        pipeline.annotate(document);
        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.word();
                // Add every word without filtering
                keywords.add(word);
            }
        }
        return keywords;
    }
    public static boolean isCoordinateLine(String line) {
        Pattern pattern = Pattern.compile("^-?\\d+\\.\\d+\t-?\\d+\\.\\d+.*");
        Matcher matcher = pattern.matcher(line);
        return matcher.find();
    }

}
