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
    private static StanfordCoreNLP arabicPipeline;

    public static void main(String[] args) {
        // Initialize NLP pipelines
        initPipelines();

        String filePath = "point_keyword_test.txt";
        parseFile(filePath);
    }
    public static void initPipelines() {
        Properties englishProps = new Properties();
        englishProps.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        englishPipeline = new StanfordCoreNLP(englishProps);

        Properties arabicProps = new Properties();
        arabicProps.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        arabicProps.setProperty("tokenize.language", "ar");
        arabicProps.setProperty("segment.model", "edu/stanford/nlp/models/segmenter/arabic/arabic-segmenter-atb+bn+arztrain.ser.gz");
        arabicPipeline = new StanfordCoreNLP(arabicProps);
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
        StanfordCoreNLP pipeline = language.equals("arabic") ? arabicPipeline : englishPipeline;

        pipeline.annotate(document);
        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.word();
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

                // Use a more lenient check for word characters, suitable for multiple languages
                if (word.matches("[\\p{L}]+") && isRelevantPOS(pos, language)) {
                    String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                    keywords.add(lemma);
                }
            }
        }
        return keywords;
    }
    public static boolean isCoordinateLine(String line) {
        Pattern pattern = Pattern.compile("^-?\\d+\\.\\d+\t-?\\d+\\.\\d+.*");
        Matcher matcher = pattern.matcher(line);
        return matcher.find();
    }
    private static boolean isRelevantPOS(String posTag, String language) {
        // Different handling for different languages if needed
        if (language.equals("english")) {
            Set<String> irrelevantPOSTags = new HashSet<>(Arrays.asList(",", ".", ":", "CC", "IN", "DT", "PRP", "PRP$"));
            return !irrelevantPOSTags.contains(posTag);
        } else {
            // For non-English languages, you might want to be more lenient or use different criteria
            return true; // For simplicity, currently allowing all POS tags for non-English languages
        }
    }
}
