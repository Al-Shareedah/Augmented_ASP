package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamingObject {
    private Set<String> keywords;
    private double x, y;
    private Map<String, ASPNode> termAspTreeNodes; // Map from term to its ASP-node

    private long zOrder; // Z-order value for the object


    public StreamingObject(Set<String> keywords, double x, double y) {
        this.keywords = keywords;
        this.x = x;
        this.y = y;
        this.termAspTreeNodes = new HashMap<>();
        this.zOrder = Synopsis.computeZOrder(x, y);
    }

    public Set<String> getAssociatedTerms() {
        return keywords;
    }

    public double getX() {
        return x;
    }


    public double getY() {
        return y;
    }


    // Method to add an ASPNode reference for a term
    public void addAspTreeNode(String term, ASPNode node) {
        this.termAspTreeNodes.put(term, node);
    }
    // Getter for termAspTreeNodes
    public Map<String, ASPNode> getTermAspTreeNodes() {
        return termAspTreeNodes;
    }

    // Getter for Z-order
    public long getZOrder() {
        return zOrder;
    }


}
