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
import org.example.BayesianNetwork;
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
    private static final double MEMORY_BUDGET_RATIO = 0.1;

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

        //frequencyCounter(object.getAssociatedTerms());

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


    private int countRepresentativeSamples(Set<StreamingObject> Lq, Set<String> keywords) {
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
    public void estimateSelectivity(Box queryBox, Set<String> queryKeywords) {
        // Step 1: Initial Checks
        if (queryKeywords.size() == 1 || queryKeywords.stream().anyMatch(keyword -> !aspTrees.containsKey(keyword))) {
            // return RCSelectivity(queryBox, queryKeywords);
        }

        // Step 2: Retrieve representative samples, TODO: change so that it would return the set of objects within query R
        Set<StreamingObject> Lq = getObjSamples(queryBox);
        int K = countRepresentativeSamples(Lq,queryKeywords );

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

        // Print parent-child relationships
        for (Map.Entry<Integer, Integer> entry : parentChildMap.entrySet()) {
            if (entry.getValue() != -1) { // Ignore the root node
                System.out.println("Parent: " + entry.getValue() + ", Child: " + entry.getKey());
            }
        }


    }


}