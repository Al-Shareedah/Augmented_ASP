package org.example;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
public class JsonParser {
    public static void main(String[] args) {
        String sourceFilePath = "C:/Users/fengw/OneDrive/Documents/Dataset/JSON_data11.txt"; // Update this to your JSON file path
        String outputFilePath = "C:/Users/fengw/OneDrive/Documents/JSON_SpatioTextual_data11.txt"; // Update this to your desired output file path

        try {
            // Read the JSON file content
            String content = new String(Files.readAllBytes(Paths.get(sourceFilePath)));

            // Split the content into separate JSON strings
            String[] jsonStrings = content.split("\n");

            // Prepare the output file
            FileWriter writer = new FileWriter(outputFilePath);

            ObjectMapper mapper = new ObjectMapper();

            for (String jsonString : jsonStrings) {
                // Skip lines containing the "limit" field
                if (jsonString.contains("\"limit\":")) {
                    continue;
                }

                if (jsonString.startsWith("{")) { // Check if it's a valid JSON object
                    JsonNode rootNode = mapper.readTree(jsonString);
                    JsonNode coordinatesNode = rootNode.path("coordinates").path("coordinates");

                    double x, y;
                    String text = rootNode.path("text").asText();

                    if (!coordinatesNode.isMissingNode() && coordinatesNode.size() > 0) {
                        // Use coordinates if available
                        x = coordinatesNode.get(0).asDouble();
                        y = coordinatesNode.get(1).asDouble();
                    } else {
                        // Calculate centroid from bounding_box if coordinates are missing or empty
                        JsonNode boundingBoxNode = rootNode.path("place").path("bounding_box").path("coordinates").get(0); // Assume first polygon for simplicity
                        if (boundingBoxNode != null && boundingBoxNode.size() > 0) {
                            double[] centroid = calculateCentroid(boundingBoxNode);
                            x = centroid[0];
                            y = centroid[1];
                        } else {
                            continue; // Skip if neither coordinates nor bounding_box are available
                        }
                    }

                    // Write to the output file
                    writer.write(x + "\t" + y + "\t" + text + "\n");
                }
            }

            writer.close();
            System.out.println("File has been written.");
        } catch (IOException e) {
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
}
