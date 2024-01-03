package org.example;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.graph.*;
import org.jgrapht.alg.spanning.*;
public class BayesianNetwork {
    public static void main(String[] args) {
        List<StreamingObject> objects = new ArrayList<>();

        // Create and add StreamingObject instances
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("disappoint", "constantly", "male", "population")), -0.76176752, 51.24830719));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("fund", "want", "have", "to", "just")), -47.85229446, -15.63417584));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("surprise", "Big", "pleasantly", "be", "Scotch", "Sound", "Ale", "drink")), -104.995, 39.7424));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("thanks", "Proud", "Tony", "North", "do", "good", "East")), -118.48101858, 33.99647333));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("PA", "overcast", "f", "Airport", "Pittsburgh", "International")), -80.24648743, 40.49546389));

        // Query keywords
        List<String> queryKeywords = Arrays.asList("disappoint", "good", "population");

        // Build Chow-Liu tree
        Graph<Integer, DefaultEdge> chowLiuTree = buildChowLiuTree(queryKeywords, objects, queryKeywords.size());

        // Print the edges and weights of the tree
        for (DefaultEdge edge : chowLiuTree.edgeSet()) {
            int source = chowLiuTree.getEdgeSource(edge);
            int target = chowLiuTree.getEdgeTarget(edge);
            double weight = chowLiuTree.getEdgeWeight(edge);

            System.out.println("Edge: " + source + " - " + target + ", Weight: " + weight);
        }

    }
    private static Map<String, Double> marginalDistribution(List<String> queryKeywords, List<StreamingObject> objects) {
        Map<String, Double> frequencies = new HashMap<>();
        for (String keyword : queryKeywords) {
            frequencies.put(keyword, 0.0);
        }

        for (StreamingObject obj : objects) {
            Set<String> keywords = obj.getAssociatedTerms();
            for (String queryKeyword : queryKeywords) {
                if (keywords.contains(queryKeyword)) {
                    frequencies.put(queryKeyword, frequencies.get(queryKeyword) + 1);
                }
            }
        }

        // Convert counts to probabilities
        for (String keyword : frequencies.keySet()) {
            frequencies.put(keyword, frequencies.get(keyword) / objects.size());
        }

        return frequencies;
    }
    private static Map<String, Double> marginalPairDistribution(List<String> queryKeywords, List<StreamingObject> objects) {
        Map<String, Double> pairFrequencies = new HashMap<>();

        // Initialize pair frequencies
        for (int i = 0; i < queryKeywords.size(); i++) {
            for (int j = i + 1; j < queryKeywords.size(); j++) {
                String pairKey = queryKeywords.get(i) + "," + queryKeywords.get(j);
                pairFrequencies.put(pairKey, 0.0);
            }
        }

        // Count occurrences of each keyword pair
        for (StreamingObject obj : objects) {
            Set<String> keywords = obj.getAssociatedTerms();
            for (int i = 0; i < queryKeywords.size(); i++) {
                for (int j = i + 1; j < queryKeywords.size(); j++) {
                    if (keywords.contains(queryKeywords.get(i)) && keywords.contains(queryKeywords.get(j))) {
                        String pairKey = queryKeywords.get(i) + "," + queryKeywords.get(j);
                        pairFrequencies.put(pairKey, pairFrequencies.get(pairKey) + 1);
                    }
                }
            }
        }

        // Convert counts to probabilities
        for (String key : pairFrequencies.keySet()) {
            pairFrequencies.put(key, pairFrequencies.get(key) / objects.size());
        }

        return pairFrequencies;
    }
    private static double calculateMutualInformation(List<String> queryKeywords, List<StreamingObject> objects, int u, int v) {
        Map<String, Double> marginalU = marginalDistribution(queryKeywords.subList(u, u + 1), objects);
        Map<String, Double> marginalV = marginalDistribution(queryKeywords.subList(v, v + 1), objects);
        List<String> pair = Arrays.asList(queryKeywords.get(u), queryKeywords.get(v));
        Map<String, Double> marginalUV = marginalPairDistribution(pair, objects);

        double I = 0.0;
        for (Map.Entry<String, Double> entryU : marginalU.entrySet()) {
            for (Map.Entry<String, Double> entryV : marginalV.entrySet()) {
                String key = entryU.getKey() + "," + entryV.getKey();
                if (marginalUV.containsKey(key)) {
                    double pXUV = marginalUV.get(key);

                    if (pXUV > 0) {
                        I += pXUV * (Math.log(pXUV) / Math.log(2) - Math.log(entryU.getValue()) / Math.log(2) - Math.log(entryV.getValue()) / Math.log(2));
                    }



                }
            }
        }
        return I;
    }
    private static Graph<Integer, DefaultEdge> buildChowLiuTree(List<String> queryKeywords, List<StreamingObject> objects, int n) {
        // Create the initial graph
        SimpleWeightedGraph<Integer, DefaultEdge> G = new SimpleWeightedGraph<>(DefaultEdge.class);
        for (int v = 0; v < n; v++) {
            G.addVertex(v);
            for (int u = 0; u < v; u++) {
                DefaultEdge edge = G.addEdge(u, v);
                double mutualInformation = calculateMutualInformation(queryKeywords, objects, u, v);
                G.setEdgeWeight(edge, -mutualInformation);
            }
        }

        // Compute the minimum spanning tree using Kruskal's algorithm
        KruskalMinimumSpanningTree<Integer, DefaultEdge> kruskal = new KruskalMinimumSpanningTree<>(G);

        // Extract the edges of the minimum spanning tree
        SpanningTreeAlgorithm.SpanningTree<DefaultEdge> spanningTree = kruskal.getSpanningTree();

        // Create a new graph for the Chow-Liu tree
        Graph<Integer, DefaultEdge> chowLiuTree = new SimpleWeightedGraph<>(DefaultEdge.class);
        for (int v = 0; v < n; v++) {
            chowLiuTree.addVertex(v);
        }

        // Add the edges from the spanning tree to the Chow-Liu tree
        for (DefaultEdge edge : spanningTree.getEdges()) {
            Integer source = G.getEdgeSource(edge);
            Integer target = G.getEdgeTarget(edge);
            DefaultEdge newEdge = chowLiuTree.addEdge(source, target);
            if (newEdge != null) {
                chowLiuTree.setEdgeWeight(newEdge, G.getEdgeWeight(edge));
            }
        }

        return chowLiuTree;
    }

}
