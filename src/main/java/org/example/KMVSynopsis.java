package org.example;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.math.BigInteger;
import java.util.*;

public class KMVSynopsis {
    private int tau; // Number of objects to maintain
    private double memoryBudget; // Memory budget B
    private PriorityQueue<StreamingObject> synopsisSet; // The overall KMV synopsis set L
    private Map<String, Synopsis> termSynopses; // Map from terms to their synopses L_i
    private HashFunction hashFunction; // The hash function h
    private Map<String, ASPTree> termASPTrees;
    private List<Double> normalizedHashValues = new ArrayList<>();
    private static final double MEMORY_BUDGET_RATIO = 0.05;

    public KMVSynopsis(double data_size) {
        this.tau = 0;
        this.memoryBudget = data_size * MEMORY_BUDGET_RATIO;
        this.synopsisSet = new PriorityQueue<>((o1, o2) -> Double.compare(hashStreamingObject(o1), hashStreamingObject(o2)));
        this.termSynopses = new HashMap<>();
        this.hashFunction = Hashing.murmur3_128();
        this.termASPTrees = new HashMap<>();
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
                termSynopses.putIfAbsent(term, new Synopsis());
                termSynopses.get(term).addObject(newObj);

                // Get the ASP tree for the term, or create it if it doesn't exist
                ASPTree tree = termASPTrees.computeIfAbsent(term, k -> new ASPTree(-180, -90, 180, 90));

                // Add the object's location to the ASP tree and get the node that contains this point
                ASPNode node = tree.putAndGetNode(newObj.getX(), newObj.getY());
                // Set the node in the StreamingObject's map for the term
                newObj.setTermNode(term, node);
            }
        }
        // Adjust the synopses if the memory budget is exceeded
        while (synopsisSet.size() > memoryBudget) {
            StreamingObject objectToRemove = synopsisSet.poll(); // Get the object with the τ-th smallest hash value
            tau--;

            // Remove the object from the synopsis for each associated term
            for (String term : objectToRemove.getAssociatedTerms()) {
                Synopsis termSynopsis = termSynopses.get(term);
                termSynopsis.removeObject(objectToRemove);

                // If the term no longer has any objects, remove the term and its associated ASP tree
                if (termSynopsis.isEmpty()) {
                    termSynopses.remove(term);
                    termASPTrees.remove(term);
                }
            }
        }

    }
    private double getCurrentThreshold() {
        return synopsisSet.isEmpty() ? Double.MAX_VALUE : hashStreamingObject(synopsisSet.peek());
    }
    // Method to hash StreamingObject's keywords and map the hash value to [0,1]
    private double hashStreamingObject(StreamingObject obj) {
        // Concatenate the keywords to create a unique string
        String keywordString = String.join(",", obj.getAssociatedTerms());
        // Hash the concatenated string and map the hash value to [0,1]
        byte[] hashBytes = hashFunction.hashUnencodedChars(keywordString).asBytes();
        BigInteger hashBigInt = new BigInteger(1, hashBytes); // '1' for positive number

        // Normalize the hash value to [0,1]
        double normalizedHash = hashBigInt.doubleValue() / BigInteger.valueOf(2).pow(128).doubleValue();
        normalizedHashValues.add(normalizedHash);
        return normalizedHash;
    }
    public Map<String, ASPTree> getTermASPTrees() {
        return termASPTrees;
    }
    public int getSynopsisSize() {
        return synopsisSet.size();
    }

    public List<Double> getNormalizedHashValues() {
        return normalizedHashValues;
    }
}
