package org.example;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class AASPTree {
    public KMVSynopsis kmvSynopsis;
    private Map<String, ASPTree> termTrees;

    public AASPTree(double data_size) throws NoSuchAlgorithmException {
        // Initialize the KMVSynopsis with the given data size
        this.kmvSynopsis = new KMVSynopsis(data_size);
        this.termTrees = kmvSynopsis.getTermASPTrees(); // Get the reference to the term ASPTrees from KMVSynopsis
    }

    public void update(StreamingObject object) {
        // Update KMV Synopsis with the new object's keywords and spatial coordinates
        kmvSynopsis.updateKMVSynopses(object);
    }

}