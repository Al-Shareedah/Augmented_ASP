package org.example;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.alg.util.UnionFind;
import org.jgrapht.graph.*;
import util.Box;

public class BayesianNetwork {
    /**
     * Calculates the marginal distribution of each keyword within a set of streaming objects.
     *
     * @param queryKeywords A set of keywords for which the marginal distribution is calculated.
     * @param objects A set of streaming objects containing associated keywords.
     * @return A map with keywords as keys and their marginal probabilities as values.
     */
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
    /**
     * Calculates the marginal pair distribution for pairs of keywords within a set of streaming objects.
     *
     * @param queryKeywords A set of keywords for which the marginal pair distribution is calculated.
     * @param objects A set of streaming objects containing associated keywords.
     * @return A map with keyword pairs as keys and their marginal probabilities as values.
     */
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
    /**
     * Calculates the mutual information between two keywords within a set of streaming objects.
     *
     * @param queryKeywords A set of keywords for which mutual information is calculated.
     * @param objects A set of streaming objects containing associated keywords.
     * @param u Index of the first keyword.
     * @param v Index of the second keyword.
     * @return The mutual information value between the two keywords.
     */
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
    /**
     * Builds a Chow-Liu tree representing the mutual information between each pair of keywords.
     *
     * @param queryKeywords A set of keywords used to build the tree.
     * @param objects A set of streaming objects containing associated keywords.
     * @param n The number of keywords.
     * @return A graph representing the Chow-Liu tree.
     */
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
/**
 * Finds the maximum spanning tree for a given graph using a custom Kruskal's algorithm.
 * @param graph The graph for which the maximum spanning tree is to be found.
 * @return A graph representing the maximum spanning tree.
 */
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
    /**
     * Finds the root node of a given tree based on the maximum degree.
     *
     * @param tree The tree for which the root node is to be found.
     * @return The index of the root node.
     */
    public static int findRootNode(Graph<Integer, DefaultEdge> tree) {
        int rootNode = -1;
        int maxDegree = 0;
        for (Integer vertex : tree.vertexSet()) {
            int degree = tree.edgesOf(vertex).size();
            if (degree > maxDegree) {
                maxDegree = degree;
                rootNode = vertex;
            }
        }
        return rootNode;
    }
    /**
     * Performs a depth-first search on a tree and maps each node to its parent.
     *
     * @param tree The tree to perform DFS on.
     * @param currentNode The current node being visited.
     * @param parentNode The parent of the current node.
     * @param parentChildMap A map for storing the parent-child relationship.
     */
    public static void depthFirstSearch(Graph<Integer, DefaultEdge> tree, int currentNode, int parentNode, Map<Integer, Integer> parentChildMap) {
        parentChildMap.put(currentNode, parentNode);
        for (DefaultEdge edge : tree.edgesOf(currentNode)) {
            int childNode = tree.getEdgeTarget(edge);
            if (childNode == currentNode) {
                childNode = tree.getEdgeSource(edge);
            }
            if (!parentChildMap.containsKey(childNode)) {
                depthFirstSearch(tree, childNode, currentNode, parentChildMap);
            }
        }
    }
    /**
     * Calculates the marginal probability for a specific keyword within a query range and set of streaming objects.
     *
     * @param keyword The keyword for which the marginal probability is calculated.
     * @param queryRange The query range within which the probability is calculated.
     * @param objects A set of streaming objects containing associated keywords.
     * @return The calculated marginal probability of the keyword.
     */
    public static double calculateMarginalProbabilityForNode(String keyword, Box queryRange,  Set<StreamingObject> objects) {
        // Use the RCSelectivity method to estimate the count of the keyword

        Set<String> queryTerms = new HashSet<>(Collections.singletonList(keyword));
        //double estimatedCount = AASPTree.RCEstimate(queryRange, queryTerms);

        // Manual count of the keyword within the query range
        int manualCount = 0;
        for (StreamingObject obj : objects) {
            if (queryRange.contains(obj.getX(), obj.getY()) && obj.getAssociatedTerms().contains(keyword)) {
                manualCount++;
            }
        }
        // Print the manual count
        System.out.println("Manual count of keyword '" + keyword + "' within the query range: " + manualCount);
        //System.out.println("Estimated count using RCEstimate '" + keyword + "' within the query range " + estimatedCount);
        // Calculate the marginal probability
        return (double) manualCount / objects.size();
    }
    /**
     * Calculates the conditional probabilities between pairs of keywords based on their parent-child relationships in a tree.
     *
     * @param queryKeywords A set of keywords for which conditional probabilities are calculated.
     * @param objects A set of streaming objects containing associated keywords.
     * @param parentChildMap A map of parent-child relationships.
     * @param tree The tree representing the relationships between keywords.
     * @return A map with keyword pairs as keys and their conditional probabilities as values.
     */
    public static Map<String, Double> calculateConditionalProbabilities(Set<String> queryKeywords, Set<StreamingObject> objects, Map<Integer, Integer> parentChildMap, Graph<Integer, DefaultEdge> tree) {
        Map<String, Double> probabilities = new HashMap<>();
        List<String> keywordList = new ArrayList<>(queryKeywords);

        for (Map.Entry<Integer, Integer> entry : parentChildMap.entrySet()) {
            int child = entry.getKey();
            int parent = entry.getValue();

            // Skip root node
            if (parent == -1) continue;

            double parentChildCount = 0;
            double parentCount = 0;

            String childKeyword = keywordList.get(child);
            String parentKeyword = keywordList.get(parent);

            for (StreamingObject obj : objects) {
                boolean parentPresent = obj.getAssociatedTerms().contains(parentKeyword);
                boolean childPresent = obj.getAssociatedTerms().contains(childKeyword);

                if (parentPresent) {
                    parentCount++;
                    if (childPresent) {
                        parentChildCount++;
                    }
                }
            }

            double conditionalProbability = parentCount == 0 ? 0 : parentChildCount / parentCount;
            probabilities.put("P( Keyword (" + childKeyword + ") | Keyword (" + parentKeyword + ") )", conditionalProbability);
        }
        return probabilities;
    }

}
