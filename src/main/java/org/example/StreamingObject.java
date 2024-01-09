package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamingObject {
    private Set<String> keywords;
    private double x, y;
    private Map<ASPNode, ASPNode> aspTreeNodes; // Map from ASP-tree to the corresponding node

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
    // Method to add a node reference from an ASPTree
    public void addNodeReference(ASPNode node) {
        aspTreeNodes.put(node, node);
    }
    // Method to get the node references
    public Map<ASPNode, ASPNode> getAspTreeNodes() {
        return aspTreeNodes;
    }
    // Getter for Z-order
    public long getZOrder() {
        return zOrder;
    }


}
