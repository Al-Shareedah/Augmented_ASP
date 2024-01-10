package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamingObject {
    private Set<String> keywords;
    private double x, y;
    private Map<StreamingObject, ASPNode> aspTreeNodes; // Map from ASP-tree to the corresponding node

    private long zOrder; // Z-order value for the object


    public StreamingObject(Set<String> keywords, double x, double y) {
        this.keywords = keywords;
        this.x = x;
        this.y = y;
        this.aspTreeNodes = new HashMap<>();
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

    // Method to add an ASPNode reference
    public void addAspTreeNode(ASPNode node) {
        this.aspTreeNodes.put(this, node); // 'this' refers to the current StreamingObject instance
    }

    // Method to get the ASPNode references
    public Map<StreamingObject, ASPNode> getAspTreeNodes() {
        return aspTreeNodes;
    }
    // Getter for Z-order
    public long getZOrder() {
        return zOrder;
    }


}
