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
    private Map<String, Integer> termFrequencies; // RC: Frequencies of terms
    private Map<String, ASPTree> aspTrees; // RC: ASP Trees for terms

    private Box space; // RC: Spatial area
    private int totalTermFrequency; // RC: Total term frequency
    private static final double MEMORY_BUDGET_RATIO = 0.05;

    public AASPTree(double data_size, Box space) throws NoSuchAlgorithmException {
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

        processPoint(object.getAssociatedTerms());

    }
    public void updateKMVSynopses(StreamingObject newObj) {
        double v = hashStreamingObject(newObj);

        // Check if the object's hash is less than the current threshold
        if (v < getCurrentThreshold()) {
            synopsisSet.add(newObj);
            tau++;

            // Update term synopses
            Set<String> associatedTerms = newObj.getAssociatedTerms();
            for (String term : associatedTerms) {
                Synopsis termSynopsis = termSynopses.computeIfAbsent(term, k -> new Synopsis());
                termSynopsis.addObject(newObj);

                // Ensure the ASPTree exists for the term
                ASPTree aspTree = aspTrees.computeIfAbsent(term, k -> new ASPTree(space.minX, space.minY, space.maxX, space.maxY));

                // Insert the point and get the corresponding ASPNode
                ASPNode aspNode = aspTree.putAndGetNode(newObj.getX(), newObj.getY());

                // Add the ASPNode reference to the StreamingObject
                if (aspNode != null) {
                    newObj.addAspTreeNode(aspNode);
                }

            }
        }
        // Adjust the synopses if the memory budget is exceeded
        while (synopsisSet.size() > memoryBudget) {
            StreamingObject objectToRemove = synopsisSet.poll(); // Get the object with the τ-th smallest hash value
            tau--;

            // Remove the object from the synopsis for each associated term
            for (String term : objectToRemove.getAssociatedTerms()) {
                if (!termSynopses.containsKey(term)) {
                    // This is a problem: the term should exist
                    System.out.println("Term not found in termSynopses: " + term);
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
    public Set<StreamingObject> queryByKeyword(String keyword) {
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
    public Map<String, Synopsis> getTermSynopses() {
        return this.termSynopses;
    }
    // Method to estimate selectivity of a query given a set of keywords and a query box
    public Set<StreamingObject> getRepSamples(Set<String> keywords, Box queryBox) {
        Set<StreamingObject> Lq = new HashSet<>(); // This will hold the union of objects matching query keywords and within the query box

        // For each query keyword, retrieve objects from term synopsis and add to Lq if within the query box
        for (String keyword : keywords) {
            Synopsis termSynopsis = termSynopses.get(keyword);
            if (termSynopsis != null) {
                for (StreamingObject obj : termSynopsis.getGridIndex().values().stream().flatMap(Set::stream).collect(Collectors.toSet())) {
                    // Check if object is within query box
                    if (queryBox.contains(obj.getX(), obj.getY())) {
                        // Check if object contains any of the keywords in the set
                        for (String key : obj.getAssociatedTerms()) {
                            if (keywords.contains(key)) {
                                Lq.add(obj);
                                break; // No need to check further keywords
                            }
                        }
                    }
                }
            }
        }



        return Lq;
    }

    private boolean isRepresentativeSample(StreamingObject obj, Set<String> keywords, Box queryBox) {
        // Check if the object is within the query box
        if (!queryBox.contains(obj.getX(), obj.getY())) {
            return false;
        }

        // Check if the object has at least one keyword from the keywords set
        for (String keyword : keywords) {
            if (obj.getAssociatedTerms().contains(keyword)) {
                return true;
            }
        }

        return false;
    }
    public void processPoint( Set<String> terms) {
        for (String term : terms) {
            termFrequencies.put(term, termFrequencies.getOrDefault(term, 0) + 1);
            totalTermFrequency++;

        }
    }

    public double RCSelectivity(Box queryRange, Set<String> queryTerms) {
        double selectivityEstimate = 1.0;

        for (String term : queryTerms) {
            double termEstimate;
            if (aspTrees.containsKey(term)) {
                termEstimate = aspTrees.get(term).estimatePointsWithin(queryRange);
            } else {
                double rho = queryRange.area() / space.area();
                termEstimate = termFrequencies.getOrDefault(term, 0) * rho;
            }
            selectivityEstimate *= termEstimate;
        }

        return selectivityEstimate;
    }

    // Method to estimate selectivity
    public double estimateSelectivity(Box queryBox, Set<String> queryKeywords, int K_threshold, BayesianNetwork bayesianNetwork) {
        // Step 1: Initial Checks
        if (queryKeywords.size() == 1 || queryKeywords.stream().anyMatch(keyword -> !aspTrees.containsKey(keyword))) {
            return RCSelectivity(queryBox, queryKeywords);
        }

        // Step 2: Retrieve representative samples, TODO: change so that it would return the set of objects within query R
        Set<StreamingObject> Lq = getRepSamples(queryKeywords, queryBox);
        int K = Lq.size();

        // Step 3: Local boosting and Chow-Liu tree construction
        if (K < K_threshold) {
            // Call local boosting method (not implemented in provided code)
            // TODO: call local boosting method
            Graph<Integer, DefaultEdge> chowLiuTree = BayesianNetwork.buildChowLiuTree(new HashSet<>(queryKeywords), Lq, queryKeywords.size());
        }else {

            Graph<Integer, DefaultEdge> chowLiuTree = BayesianNetwork.buildChowLiuTree(new HashSet<>(queryKeywords), Lq, queryKeywords.size());
        }

        // Step 4: Calculate selectivity
        double theta = 1.0;
        int N = K;



        // Final selectivity estimation
        return N * theta;
    }


}