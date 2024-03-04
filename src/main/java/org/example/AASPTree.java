package org.example;

import util.Box;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.jgrapht.*;

import org.jgrapht.graph.*;


public class AASPTree {
    private int tau; // KMV: Number of objects to maintain
    private double memoryBudget; // KMV: Memory budget
    private PriorityQueue<StreamingObject> synopsisSet; // KMV: The overall KMV synopsis set
    private Map<String, Synopsis> termSynopses; // KMV: Map from terms to their synopses
    private static Map<String, Integer> termFrequencies; // RC: Frequencies of terms
    private static Map<String, ASPTree> aspTrees; // RC: ASP Trees for terms
    private static int totalNumObjects;
    private static Box space; // RC: Spatial area
    private int totalTermFrequency; // RC: Total term frequency
    private static final double MEMORY_BUDGET_RATIO = 0.1;

    public AASPTree(double data_size, Box space) {
        // KMV Initialization
        this.tau = 0;
        this.memoryBudget = data_size * MEMORY_BUDGET_RATIO;
        this.synopsisSet = new PriorityQueue<>((o1, o2) -> Double.compare(hashStreamingObject(o1), hashStreamingObject(o2)));
        this.termSynopses = new HashMap<>();

        // RC Initialization

        this.space = space;
        this.termFrequencies = new HashMap<>();
        this.aspTrees = new HashMap<>();
        this.totalTermFrequency = 0;
    }
    public void update(StreamingObject object) {
        // KMV Update
        updateKMVSynopses(object);
        incrementTotalNumObjects();

    }
    public void updateKMVSynopses(StreamingObject newObj) {
        double v = hashStreamingObject(newObj);

        boolean addedToSynopsis = false;

        if (v < getCurrentThreshold()) {
            synopsisSet.add(newObj);
            tau++;
            addedToSynopsis = true;

            // Update term synopses and ASP trees
            for (String term : newObj.getAssociatedTerms()) {
                // Update term synopses
                Synopsis termSynopsis = termSynopses.computeIfAbsent(term, k -> new Synopsis());
                termSynopsis.addObject(newObj);

                // Check if an ASP tree exists for the term
                ASPTree aspTree = aspTrees.get(term);
                if (aspTree == null) {
                    // Create a new ASP tree if it doesn't exist
                    aspTree = new ASPTree(space.minX, space.minY, space.maxX, space.maxY);
                    aspTrees.put(term, aspTree);
                }

                // Add the new object's node to the ASP tree
                aspTree.putAndGetNode(newObj.getX(), newObj.getY());
            }
        }

        // Call frequencyCounter if object is not added to synopsisSet
        if (!addedToSynopsis) {
            frequencyCounter(newObj.getAssociatedTerms());
        }

        // Adjust the synopses if the memory budget is exceeded
        while (synopsisSet.size() > memoryBudget) {
            StreamingObject objectToRemove = synopsisSet.poll(); // Get the object with the τ-th smallest hash value
            tau--;

            // Remove the object from the synopsis for each associated term
            for (String term : objectToRemove.getAssociatedTerms()) {
                if (!termSynopses.containsKey(term)) {

                    continue; // Skip this term to avoid NullPointerException
                }
                Synopsis termSynopsis = termSynopses.get(term);
                termSynopsis.removeObject(objectToRemove);

                // If the term no longer has any objects, remove the term
                if (termSynopsis.isEmpty()) {
                    termSynopses.remove(term);
                }
            }
        }

    }
    private double getCurrentThreshold() {
        // Check if the synopsis set size is larger than the memory budget
        if (synopsisSet.size() > memoryBudget) {
            // Return the smallest hash value in the synopsis set
            return hashStreamingObject(synopsisSet.peek());
        } else {
            // Return MAX_VALUE when the synopsis set size is within the memory budget
            return Double.MAX_VALUE;
        }
    }
    // Method to hash StreamingObject's keywords and map the hash value to [0,1]
    private double hashStreamingObject(StreamingObject obj) {
        // Concatenate the keywords to create a unique string
        String keywordString = String.join(",", obj.getAssociatedTerms());

        // Hash the concatenated string using SHA-256
        byte[] hashBytes = sha256(keywordString);
        BigInteger hashBigInt = new BigInteger(1, hashBytes); // '1' for positive number

        // Normalize the hash value to [0,1]
        double normalizedHash = hashBigInt.doubleValue() / BigInteger.valueOf(2).pow(256).doubleValue();

        return normalizedHash;
    }

    private byte[] sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    // Additional helper method to determine if an ASPTree should be maintained for a term
    private boolean shouldMaintainASPTree(String term) {
        return termSynopses.containsKey(term);
    }

    public int getSynopsisSize() {
        return synopsisSet.size();
    }


    // Method to query objects by keyword using the grid-based inverted index
    public Set<StreamingObject> DVEstimator(String keyword) {
        Set<StreamingObject> result = new HashSet<>();
        Synopsis termSynopsis = termSynopses.get(keyword);
        if (termSynopsis != null) {
            // Debugging statement to check if termSynopsis is correctly populated
            System.out.println("Synopsis found for keyword: " + keyword + ", number of objects: " + termSynopsis.getGridIndex().size());

            for (Map.Entry<Long, SortedSet<StreamingObject>> entry : termSynopsis.getGridIndex().entrySet()) {
                for (StreamingObject obj : entry.getValue()) {
                    // Debugging statement to check each object
                    System.out.println("Checking object with keywords: " + obj.getAssociatedTerms());

                    if (obj.getAssociatedTerms().contains(keyword)) {
                        result.add(obj);
                    }
                }
            }
        } else {
            // Debugging statement if no synopsis is found for the keyword
            System.out.println("No synopsis found for keyword: " + keyword);
        }
        return result;
    }

    // Method to estimate selectivity of a query given a set of keywords and a query box
    public Set<StreamingObject> getObjSamples(Box queryBox) {
        Set<StreamingObject> Lq = new HashSet<>(); // This will hold the objects within the query box

        // Iterate over all term synopses
        for (Map.Entry<String, Synopsis> entry : termSynopses.entrySet()) {
            Synopsis termSynopsis = entry.getValue();
            if (termSynopsis != null) {
                for (StreamingObject obj : termSynopsis.getGridIndex().values().stream().flatMap(Set::stream).collect(Collectors.toSet())) {
                    // Check if object is within query box
                    if (queryBox.contains(obj.getX(), obj.getY())) {
                        Lq.add(obj);
                    }
                }
            }
        }

        return Lq;
    }


    public int countRepresentativeSamples(Set<StreamingObject> Lq, Set<String> keywords) {
        int representativeSampleCount = 0;

        for (StreamingObject obj : Lq) {
            // Check if the object has at least one keyword from the keywords set
            for (String keyword : keywords) {
                if (obj.getAssociatedTerms().contains(keyword)) {
                    representativeSampleCount++;
                    break; // No need to check further keywords for this object
                }
            }
        }

        return representativeSampleCount;
    }

    public void frequencyCounter(Set<String> terms) {
        for (String term : terms) {
            termFrequencies.put(term, termFrequencies.getOrDefault(term, 0) + 1);
            totalTermFrequency++;

        }
    }

    public static double RCEstimate(Box queryRange, Set<String> queryTerms) {
        int n = getTotalNumObjects(); // Use the counter as the total number of objects
        double rho = queryRange.area() / space.area();
        double product = 1.0;

        for (String term : queryTerms) {
            double Ai;
            if (aspTrees.containsKey(term)) {
                Ai = aspTrees.get(term).estimatePointsWithin(queryRange);
            } else {
                int ni = termFrequencies.getOrDefault(term, 0);
                Ai = ni * rho;
            }
            product *= Ai / n;
        }

        return n * product;
    }
    public void incrementTotalNumObjects() {
        totalNumObjects++;
    }
    // Getter method for insertionAttempts
    public static int getTotalNumObjects() {
        return totalNumObjects;
    }

    // Method to estimate selectivity
    public double estimateSelectivity(Box queryBox, Set<String> queryKeywords, int K_threshold) {
        // Step 1: Initial Checks
        if (queryKeywords.size() == 1 || queryKeywords.stream().anyMatch(keyword -> !aspTrees.containsKey(keyword))) {
             return RCEstimate(queryBox, queryKeywords);
        }
        // Initialize marginal and conditional probabilities maps
        Map<String, Double> marginalProbabilities = new HashMap<>();
        Map<String, Double> conditionalProbabilities = new HashMap<>();
        String rootKeyword;

        // Step 2: Retrieve representative samples
        Set<StreamingObject> Lq = getObjSamples(queryBox);
        int K = countRepresentativeSamples(Lq,queryKeywords);

        // Check if K is less than K threshold
        if ( K < K_threshold){
            // Call localBoosting to potentially find a better query box
            Box boostedQueryBox = localBoosting(queryKeywords, queryBox, K);
            Set<StreamingObject> new_Lq = getObjSamples(boostedQueryBox);
            // Learn graph structure based on the new query region
            Graph<Integer, DefaultEdge> chowLiuTree = BayesianNetwork.buildChowLiuTree(new HashSet<>(queryKeywords), new_Lq, queryKeywords.size());
            // Print the edges and weights of the tree
            for (DefaultEdge edge : chowLiuTree.edgeSet()) {
                int source = chowLiuTree.getEdgeSource(edge);
                int target = chowLiuTree.getEdgeTarget(edge);
                double weight = chowLiuTree.getEdgeWeight(edge);

                System.out.println("Edge: " + source + " - " + target + ", Weight: " + weight);
            }
            // Identify the root node
            int rootNode = BayesianNetwork.findRootNode(chowLiuTree);

            // Perform depth-first search from root node
            Map<Integer, Integer> parentChildMap = new HashMap<>();
            BayesianNetwork.depthFirstSearch(chowLiuTree, rootNode, -1, parentChildMap);

            // Calculate marginal probability for the root node
            rootKeyword = new ArrayList<>(queryKeywords).get(rootNode);
            // calculate marginal probability for parent nodes on the original query region
            marginalProbabilities.put(rootKeyword, BayesianNetwork.calculateMarginalProbabilityForNode(rootKeyword, queryBox, Lq));

            // Calculate conditional probabilities for non-root nodes on the boosted region
            conditionalProbabilities = BayesianNetwork.calculateConditionalProbabilities(queryKeywords, new_Lq, parentChildMap, chowLiuTree);

        }
        else{
            // Learn graph structure based on the new query region
            Graph<Integer, DefaultEdge> chowLiuTree = BayesianNetwork.buildChowLiuTree(new HashSet<>(queryKeywords), Lq, queryKeywords.size());
            // Print the edges and weights of the tree
            for (DefaultEdge edge : chowLiuTree.edgeSet()) {
                int source = chowLiuTree.getEdgeSource(edge);
                int target = chowLiuTree.getEdgeTarget(edge);
                double weight = chowLiuTree.getEdgeWeight(edge);

                System.out.println("Edge: " + source + " - " + target + ", Weight: " + weight);
            }
            // Identify the root node
            int rootNode = BayesianNetwork.findRootNode(chowLiuTree);

            // Perform depth-first search from root node
            Map<Integer, Integer> parentChildMap = new HashMap<>();
            BayesianNetwork.depthFirstSearch(chowLiuTree, rootNode, -1, parentChildMap);

            // Calculate marginal probability for the root node
            rootKeyword = new ArrayList<>(queryKeywords).get(rootNode);
            // calculate marginal probability for parent nodes on the original query region
            marginalProbabilities.put(rootKeyword, BayesianNetwork.calculateMarginalProbabilityForNode(rootKeyword, queryBox, Lq));

            // Calculate conditional probabilities for non-root nodes on the boosted region
            conditionalProbabilities = BayesianNetwork.calculateConditionalProbabilities(queryKeywords, Lq, parentChildMap, chowLiuTree);

        }

        System.out.println("Marginal Probability for Root Node:");
        for (Map.Entry<String, Double> entry : marginalProbabilities.entrySet()) {
            System.out.println("Keyword ("+ entry.getKey() + ") : " + entry.getValue());
        }

        System.out.println("Conditional Probabilities for Non-Root Nodes:");
        for (Map.Entry<String, Double> entry : conditionalProbabilities.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        // Initialize theta
        double theta = 1.0;

        // Multiply all conditional probabilities by theta
        // Calculate the product of all conditional probabilities
        double conditionalProbProduct = 1.0;
        for (Map.Entry<String, Double> entry : conditionalProbabilities.entrySet()) {
            conditionalProbProduct *= entry.getValue();
        }
        theta *= conditionalProbProduct;
        // Multiply theta by marginal probability of the root node
        if (marginalProbabilities.containsKey(rootKeyword)) {
            theta *= marginalProbabilities.get(rootKeyword);
        }

        // Multiply theta by K
        theta *= K;
        System.out.println("Final Theta Value: " + theta);

        return theta;
    }
    public Box localBoosting(Set<String> queryKeywords, Box queryBox, int K) {
        Box bestBox = queryBox;
        int bestK = K;
        System.out.println("Original K is: " + bestK);


        for (String keyword : queryKeywords) {
            if (!aspTrees.containsKey(keyword)) {
                continue;
            }

            ASPTree aspTree = aspTrees.get(keyword);
            ASPNode node = aspTree.getNodeContaining(queryBox);


            if (node.getBounds() != null) {

                Set<StreamingObject> objSamples = getObjSamples(node.getBounds());
                int newK = countRepresentativeSamples(objSamples, queryKeywords);

                if (newK > bestK) {
                    bestK = newK;
                    bestBox = node.getBounds();

                }
            }
        }

        System.out.println("The best K found is: " + bestK);
        System.out.println("The box for the best K is: " + bestBox.toString());
        return bestBox;
    }




}