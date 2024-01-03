package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StreamingObject {
    private List<String> keywords;
    private double x, y;
    private Map<String, ASPNode> termToNodeMap; // Maps terms to ASP nodes


    public StreamingObject(List<String> keywords, double x, double y) {
        this.keywords = keywords;
        this.x = x;
        this.y = y;
        this.termToNodeMap = new HashMap<>();
    }

    public List<String> getAssociatedTerms() {
        return keywords;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
    public ASPNode getTermNode(String term) {
        return termToNodeMap.get(term);
    }
    public void setTermNode(String term, ASPNode node) {
        termToNodeMap.put(term, node);
    }

}
