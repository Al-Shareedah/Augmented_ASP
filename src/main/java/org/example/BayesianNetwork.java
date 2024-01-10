package org.example;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.util.UnionFind;
import org.jgrapht.graph.*;
import org.jgrapht.alg.spanning.*;
public class BayesianNetwork {
    public static void main(String[] args) {
        Set<StreamingObject> objects = new HashSet<>();

        // Create and add StreamingObject instances
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("disappoint", "constantly", "male", "population")), -0.76176752, 51.24830719));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("fund", "want", "have", "to", "just")), -47.85229446, -15.63417584));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("surprise", "Big", "pleasantly", "be", "Scotch", "Sound", "Ale", "drink")), -104.995, 39.7424));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("thanks", "Proud", "Tony", "Big", "North", "do", "good", "East")), -118.48101858, 33.99647333));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("PA", "overcast", "f", "Airport", "Pittsburgh", "International")), -80.24648743, 40.49546389));

        // Query keywords
        Set<String> queryKeywords = new HashSet<>(Arrays.asList("disappoint", "good", "population", "Big"));

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
    private static Map<String, Double> marginalDistribution(Set<String> queryKeywords, Set<StreamingObject> objects) {
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
    private static Map<String, Double> marginalPairDistribution(Set<String> queryKeywords, Set<StreamingObject> objects) {
        Map<String, Double> pairFrequencies = new HashMap<>();

        // Initialize pair frequencies
        List<String> keywordList = new ArrayList<>(queryKeywords);
        for (int i = 0; i < keywordList.size(); i++) {
            for (int j = i + 1; j < keywordList.size(); j++) {
                String pairKey = keywordList.get(i) + "," + keywordList.get(j);
                pairFrequencies.put(pairKey, 0.0);
            }
        }

        // Count occurrences of each keyword pair
        for (StreamingObject obj : objects) {
            Set<String> keywords = obj.getAssociatedTerms();
            for (int i = 0; i < keywordList.size(); i++) {
                for (int j = i + 1; j < keywordList.size(); j++) {
                    if (keywords.contains(keywordList.get(i)) && keywords.contains(keywordList.get(j))) {
                        String pairKey = keywordList.get(i) + "," + keywordList.get(j);
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
    private static double calculateMutualInformation(Set<String> queryKeywords, Set<StreamingObject> objects, int u, int v) {
        List<String> keywordList = new ArrayList<>(queryKeywords);
        Set<String> marginalUKeywords = new HashSet<>(Collections.singletonList(keywordList.get(u)));
        Set<String> marginalVKeywords = new HashSet<>(Collections.singletonList(keywordList.get(v)));
        Set<String> pairKeywords = new HashSet<>(Arrays.asList(keywordList.get(u), keywordList.get(v)));

        Map<String, Double> marginalU = marginalDistribution(marginalUKeywords, objects);
        Map<String, Double> marginalV = marginalDistribution(marginalVKeywords, objects);
        Map<String, Double> marginalUV = marginalPairDistribution(pairKeywords, objects);

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
    public static Graph<Integer, DefaultEdge> buildChowLiuTree(Set<String> queryKeywords, Set<StreamingObject> objects, int n) {
        // Create the initial graph
        SimpleWeightedGraph<Integer, DefaultEdge> G = new SimpleWeightedGraph<>(DefaultEdge.class);
        for (int v = 0; v < n; v++) {
            G.addVertex(v);
            for (int u = 0; u < v; u++) {
                DefaultEdge edge = G.addEdge(u, v);
                double mutualInformation = calculateMutualInformation(queryKeywords, objects, u, v);
                G.setEdgeWeight(edge, mutualInformation);
            }
        }

        // Custom Kruskal's algorithm to find the maximum spanning tree
        return findMaximumSpanningTree(G);
    }
    private static Graph<Integer, DefaultEdge> findMaximumSpanningTree(SimpleWeightedGraph<Integer, DefaultEdge> graph) {
        Graph<Integer, DefaultEdge> maxSpanningTree = new SimpleWeightedGraph<>(DefaultEdge.class);

        // Add all vertices to the new graph
        for (Integer vertex : graph.vertexSet()) {
            maxSpanningTree.addVertex(vertex);
        }

        // Sort edges in decreasing order of weights
        List<DefaultEdge> edges = new ArrayList<>(graph.edgeSet());
        edges.sort((e1, e2) -> Double.compare(graph.getEdgeWeight(e2), graph.getEdgeWeight(e1)));

        // Union-find structure to detect cycles
        UnionFind<Integer> unionFind = new UnionFind<>(graph.vertexSet());

        // Process edges from highest to lowest weight
        for (DefaultEdge edge : edges) {
            int source = graph.getEdgeSource(edge);
            int target = graph.getEdgeTarget(edge);

            // Add edge if it doesn't form a cycle
            if (!Objects.equals(unionFind.find(source), unionFind.find(target))) {
                maxSpanningTree.addEdge(source, target);
                maxSpanningTree.setEdgeWeight(source, target, graph.getEdgeWeight(edge));
                unionFind.union(source, target);
            }
        }

        return maxSpanningTree;
    }

}
