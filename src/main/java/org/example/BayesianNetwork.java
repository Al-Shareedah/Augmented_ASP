package org.example;
import java.util.*;
import org.jgrapht.*;
import org.jgrapht.alg.interfaces.SpanningTreeAlgorithm;
import org.jgrapht.alg.util.UnionFind;
import org.jgrapht.graph.*;
import org.jgrapht.alg.spanning.*;
import util.Box;

public class BayesianNetwork {
    public static void main(String[] args) {
        Set<StreamingObject> objects = new HashSet<>();

        // Create and add StreamingObject instances
        // Create and add StreamingObject instances
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("that", "at", "@victoriarguzzo", "Lol", "Snapchat")), -77.67470247, 37.40286992));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("hungry", "I", "so")), -82.92235885, 39.939383));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("face", "painted", "Getting", "my")), -82.1413294, 29.1823833));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("boring", "this", "is", "Wow")), -85.28317083, 42.65822559));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("a", "fonddd", "Ma", "banniere", "lourde")), 1.8378976, 48.761995999999996));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("q", "Ahora", "nos", "con", "la", "mis", "viejo", "tomamos", "llego", "una", "birra", "nonaa")), -64.1982668, -31.399994));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("at", "Dayı", "Plaza", "I", "Köylü", "http://t.co/nbl2tSsexi")), 34.57631558, 36.78668099));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("uuuuuuuuh", "monica")), -49.28839825, -25.4952494));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("de", "a", "el", "bloques", "ir", "del", "pensar", "putada", "caerse", "cn", "Viciada", "obsidiana", "conseguir", "sería", "ahora", "siglo", "pico", "q", "por", "lava", "la", "si", "diamantes")), -4.40527915, 40.36117332));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("be", "grow", "wan", "part", "was", "I", "best", "when", "the", "@Draplin", "what", "na", "of", "Now", "know", "up")), -93.24741524, 44.86743921));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("think", "be", "should", "I", "asleep", "rn", "really")), -2.74114134, 56.05831074));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("Take", "Red", "https://t.co/vhZuJXLQgz", "by", "Me", "Midnight", "Home")), 107.00687, -6.18151));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("Gääähhn", "offline", "Nacht", "müde", "bin", "Bin", "Morgen", "mal", "Gute", "bis", "Ich")), 7.4836054999999995, 50.053081));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("com", "mt", "to", "alergia", "Mds")), -43.58418373, -22.90743773));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("Bnocjeeeeees")), -2.4338753, 36.8659131));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("at", "@BalHarbourShops", "FL", "Harbour", "I", "http://t.co/VqD6po57fL", "Bal")), -80.12432897, 25.88839312));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("That", "the", "@CrimsonFlavor", "how", "in", "clutch", "do", "we")), -82.5051845, 33.4595586));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("Trynna", "get", "w", "friend", "your")), -97.31554113, 37.78954486));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("sesat", "aliran", "niii", "tidak", "@PutriGandhiIA", "badah", "patutu", "ditiru")), 115.22774061, -8.65398449));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("que", "de", "a", "viu", "pensava", "milllll", "firma", "ta", "eu", "nave", "caiuuu", "me", "ela", "passei", "desacreditada")), -43.791562, -21.247522099999998));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("weird", "haha", "hm", "came", "oh", "to", "@KatieGrothiepoo", "he", "us")), -80.0412875, 40.3948936));
        objects.add(new StreamingObject(new HashSet<>(Arrays.asList("Sumber", "Resort", "Alam", "pic", "Hollaa", "error", "path", "masih", "kah", "at", "pagiii", "https://t.co/wcn2q1FaR9", "Kampung", "selamat", "garuutt")), 107.8755, -7.19163));

        // Query keywords (example, modify as needed)
        Set<String> queryKeywords = new HashSet<>(Arrays.asList("the", "I", "do"));



        // Build Chow-Liu tree
        Graph<Integer, DefaultEdge> chowLiuTree = buildChowLiuTree(queryKeywords, objects, queryKeywords.size());

        // Print the edges and weights of the tree
        for (DefaultEdge edge : chowLiuTree.edgeSet()) {
            int source = chowLiuTree.getEdgeSource(edge);
            int target = chowLiuTree.getEdgeTarget(edge);
            double weight = chowLiuTree.getEdgeWeight(edge);

            System.out.println("Edge: " + source + " - " + target + ", Weight: " + weight);
        }

        // Identify the root node
        int rootNode = findRootNode(chowLiuTree);

        // Perform depth-first search from root node
        Map<Integer, Integer> parentChildMap = new HashMap<>();
        depthFirstSearch(chowLiuTree, rootNode, -1, parentChildMap);

        // Calculate marginal probability for the root node
        Map<String, Double> marginalProbabilities = new HashMap<>();
        String rootKeyword = new ArrayList<>(queryKeywords).get(rootNode);
    //    marginalProbabilities.put(rootKeyword, calculateMarginalProbabilityForNode(rootKeyword, objects));

        // Calculate conditional probabilities for non-root nodes
        Map<String, Double> conditionalProbabilities = calculateConditionalProbabilities(queryKeywords, objects, parentChildMap, chowLiuTree);

        System.out.println("Marginal Probability for Root Node:");
        for (Map.Entry<String, Double> entry : marginalProbabilities.entrySet()) {
            System.out.println("Keyword ("+ entry.getKey() + ") : " + entry.getValue());
        }

        System.out.println("Conditional Probabilities for Non-Root Nodes:");
        for (Map.Entry<String, Double> entry : conditionalProbabilities.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
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

    public static double calculateMarginalProbabilityForNode(String keyword, Box queryRange,  Set<StreamingObject> objects) {
        // Use the RCSelectivity method to estimate the count of the keyword

        Set<String> queryTerms = new HashSet<>(Collections.singletonList(keyword));
        double estimatedCount = AASPTree.RCEstimate(queryRange, queryTerms);

        // Manual count of the keyword within the query range
        int manualCount = 0;
        for (StreamingObject obj : objects) {
            if (queryRange.contains(obj.getX(), obj.getY()) && obj.getAssociatedTerms().contains(keyword)) {
                manualCount++;
            }
        }
        // Print the manual count
        System.out.println("Manual count of keyword '" + keyword + "' within the query range: " + manualCount);
        System.out.println("Estimated count using RCEstimate '" + keyword + "' within the query range " + estimatedCount);
        // Calculate the marginal probability
        return estimatedCount / objects.size();
    }
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
