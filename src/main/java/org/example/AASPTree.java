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
    private static Map<String, ASPTree> rceASPTrees;
    private Map<String, Double> ASPalpha; // Track alpha values for ASPTrees

    private static int totalNumObjects;
    private static Box space; // RC: Spatial area
    private int totalTermFrequency; // RC: Total term frequency
    private static final double MEMORY_BUDGET_RATIO = 0.45;
    private static final double EPSILON = 0.1;

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
        this.rceASPTrees= new HashMap<>();
        this.ASPalpha = new HashMap<>();
    }
    public void update(StreamingObject object) {
        // KMV Update
        updateKMVSynopses(object);
        updateRCE(object);
        incrementTotalNumObjects();

    }
    public void updateKMVSynopses(StreamingObject newObj) {
        double v = hashStreamingObject(newObj);

        boolean addedToSynopsis = false;

        if (v < getCurrentThreshold()) {
            synopsisSet.add(newObj);
            tau++;
            addedToSynopsis = true;
            // check if keywords exist or not
            if (newObj.getAssociatedTerms() != null && !newObj.getAssociatedTerms().isEmpty()) {
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

        }

        // Adjust the synopses if the memory budget is exceeded
        while (synopsisSet.size() > memoryBudget) {
            StreamingObject objectToRemove = synopsisSet.poll(); // Get the object with the τ-th smallest hash value
            tau--;

            // Check if the keywords set is not null
            if (objectToRemove.getAssociatedTerms() != null) {
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

    }
    private void updateRCE(StreamingObject newObj) {
        // Record all terms and update term frequencies
        Set<String> associatedTerms = newObj.getAssociatedTerms();
        if (associatedTerms != null) {
            for (String term : associatedTerms) {
                int previousFrequency = termFrequencies.getOrDefault(term, 0);
                termFrequencies.put(term, previousFrequency + 1);
                totalTermFrequency++;

                boolean isUnderMemoryBudget = rceASPTrees.size() <= memoryBudget;
                double threshold = EPSILON * totalTermFrequency;
                if (isUnderMemoryBudget || termFrequencies.get(term) > threshold) {
                    if (!rceASPTrees.containsKey(term)) {
                        // If the term doesn't have an ASP Tree yet, create one with alpha set to 0
                        ASPTree newAspTree = new ASPTree(space.minX, space.minY, space.maxX, space.maxY);
                        newAspTree.setAlpha(0.0); // Set initial alpha to 0
                        rceASPTrees.put(term, newAspTree);
                    }

                    ASPTree aspTree = rceASPTrees.get(term);
                    aspTree.putAndGetNode(newObj.getX(), newObj.getY());

                    // Adjust alpha only if term frequency has increased
                    if (termFrequencies.get(term) > previousFrequency) {
                        double increment = calculateAlphaIncrement(termFrequencies.get(term) - previousFrequency);
                        double newAlpha = ASPalpha.getOrDefault(term, 0.0) + increment; // Initialize with 0.0 for new terms
                        aspTree.setAlpha(newAlpha);
                        ASPalpha.put(term, newAlpha);
                    }
                }
            }
        }
    }
    private double calculateAlphaIncrement(int termFrequency) {
        // Increase alpha by 0.01 for every 10-term frequency
        return (double)termFrequency / 10 * 0.01;
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
        String keywordString;
        // Check if keywords are null or empty
        if (obj.getAssociatedTerms() == null || obj.getAssociatedTerms().isEmpty()) {
            // If keywords are empty, hash the coordinates
            keywordString = obj.getX() + "," + obj.getY();
        }else {
            // Concatenate the keywords to create a unique string
            keywordString = String.join(",", obj.getAssociatedTerms());
        }


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

                    if (obj.getAssociatedTerms() != null && obj.getAssociatedTerms().contains(keyword)) {
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
    public Set<StreamingObject> getObjSamples(Box queryBox, Set<String> queryKeywords) {
        Set<StreamingObject> Lq = new HashSet<>(); // This will hold the objects within the query box

        // Iterate over all term synopses
        for (Map.Entry<String, Synopsis> entry : termSynopses.entrySet()) {
            String term = entry.getKey();
            // Check if the current term is part of the query keywords
            if (queryKeywords.contains(term)) {
                Synopsis termSynopsis = entry.getValue();
                if (termSynopsis != null) {
                    for (StreamingObject obj : termSynopsis.getGridIndex().values().stream().flatMap(Set::stream).collect(Collectors.toSet())) {
                        // Check if object is within query box and has at least one of the query keywords
                        if (queryBox.contains(obj.getX(), obj.getY()) && obj.getKeywords().stream().anyMatch(queryKeywords::contains)) {
                            Lq.add(obj);
                        }
                    }
                }
            }
        }

        return Lq;
    }


    public int countRepresentativeSamples(Set<StreamingObject> Lq, Set<String> keywords) {
        int representativeSampleCount = 0;

        for (StreamingObject obj : Lq) {
            if (obj.getAssociatedTerms() == null) {
                continue; // Skip if associated terms are null
            }
            // Check if the object has all keywords from the keywords set
            if (obj.getAssociatedTerms().containsAll(keywords)) {
                representativeSampleCount++;
            }
        }

        return representativeSampleCount;
    }

    public void frequencyCounter(Set<String> terms) {
        if (terms == null) {
            return; // Skip if terms are null
        }
        for (String term : terms) {

            termFrequencies.put(term, termFrequencies.getOrDefault(term, 0) + 1);
            totalTermFrequency++;

        }
    }

    public double RCEstimate(Box queryRange, Set<String> queryTerms) {
        int n = totalNumObjects; // Use the counter as the total number of objects
        double rho = queryRange.area() / space.area();
        double product = 1.0;

        for (String term : queryTerms) {
            double Ai;
            if (rceASPTrees.containsKey(term)) {
                Ai = rceASPTrees.get(term).estimatePointsWithin(queryRange);
            } else {
                double ni = termFrequencies.getOrDefault(term, 0);
                Ai = ni * rho;
            }
            product *= Ai / n;
        }

        return n * product;
    }
    // Call this method every time an object insertion is attempted
    public void incrementTotalNumObjects() {
        totalNumObjects++;
    }
    // Getter method for insertionAttempts
    public static int getTotalNumObjects() {
        return totalNumObjects;
    }
    // Method to estimate selectivity
    public double estimateSelectivity(Box queryBox, Set<String> queryKeywords, int K_threshold) {
        // Check if queryKeywords is empty and return 0 immediately
        if (queryKeywords.isEmpty() || queryKeywords == null) {
            return 0;
        }
        // Step 1: Initial Checks
        // If there is only one keyword, call RCEstimate
        if (queryKeywords.size() == 1) {
            return RCEstimate(queryBox, queryKeywords);
        }

        // Check each keyword individually
        for (String keyword : queryKeywords) {
            if (!aspTrees.containsKey(keyword)) {
                // If any keyword is not in aspTrees, call RCEstimate
                return RCEstimate(queryBox, queryKeywords);
            }
        }


        // Initialize marginal and conditional probabilities maps
        Map<String, Double> marginalProbabilities = new HashMap<>();
        Map<String, Double> conditionalProbabilities = new HashMap<>();
        String rootKeyword;

        // Step 2: Retrieve representative samples
        Set<StreamingObject> Lq = getObjSamples(queryBox, queryKeywords);
        int K = countRepresentativeSamples(Lq,queryKeywords);

        // Check if K is less than K threshold
        if ( K < K_threshold){
            // Call localBoosting to potentially find a better query box
            Box boostedQueryBox = localBoosting(queryKeywords, queryBox, K);
            Set<StreamingObject> new_Lq = getObjSamples(boostedQueryBox, queryKeywords);
            K = countRepresentativeSamples(new_Lq,queryKeywords);
            // Learn graph structure based on the new query region
            Graph<Integer, DefaultEdge> chowLiuTree = BayesianNetwork.buildChowLiuTree(new HashSet<>(queryKeywords), new_Lq, queryKeywords.size());
            /*
            // Print the edges and weights of the tree
            for (DefaultEdge edge : chowLiuTree.edgeSet()) {
                int source = chowLiuTree.getEdgeSource(edge);
                int target = chowLiuTree.getEdgeTarget(edge);
                double weight = chowLiuTree.getEdgeWeight(edge);

              System.out.println("Edge: " + source + " - " + target + ", Weight: " + weight);
            }

             */
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
            /*
            // Print the edges and weights of the tree
            for (DefaultEdge edge : chowLiuTree.edgeSet()) {
                int source = chowLiuTree.getEdgeSource(edge);
                int target = chowLiuTree.getEdgeTarget(edge);
                double weight = chowLiuTree.getEdgeWeight(edge);

                System.out.println("Edge: " + source + " - " + target + ", Weight: " + weight);
            }

             */
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

        /*System.out.println("Marginal Probability for Root Node:");
        for (Map.Entry<String, Double> entry : marginalProbabilities.entrySet()) {
            System.out.println("Keyword ("+ entry.getKey() + ") : " + entry.getValue());
        }

        System.out.println("Conditional Probabilities for Non-Root Nodes:");
        for (Map.Entry<String, Double> entry : conditionalProbabilities.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

         */
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
        theta *= Lq.size();
        //System.out.println("Final Theta Value: " + theta);

        return theta;
    }
    public Box localBoosting(Set<String> queryKeywords, Box queryBox, int K) {
        Box bestBox = queryBox;
        int bestK = K;
        //System.out.println("Original K is: " + bestK);


        for (String keyword : queryKeywords) {
            if (!aspTrees.containsKey(keyword)) {
                continue;
            }

            ASPTree aspTree = aspTrees.get(keyword);
            ASPNode node = aspTree.getNodeContaining(queryBox);


            if (node.getBounds() != null) {

                Set<StreamingObject> objSamples = getObjSamples(node.getBounds(), queryKeywords);
                int newK = countRepresentativeSamples(objSamples, queryKeywords);

                if (newK > bestK) {
                    bestK = newK;
                    bestBox = node.getBounds();

                }
            }
        }

        //  System.out.println("The best K found is: " + bestK);
        // System.out.println("The box for the best K is: " + bestBox.toString());
        return bestBox;
    }




}