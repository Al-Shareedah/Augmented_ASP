package org.example;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import util.Box;
public class RCEstimator {
    private final Map<String, Integer> termFrequencies = new HashMap<>();
    private final Map<String, ASPTree> aspTrees = new HashMap<>();
    private final double epsilon;
    private final Box space;
    private int totalTermFrequency = 0;

    public RCEstimator(double epsilon, Box space) {
        this.epsilon = epsilon;
        this.space = space;
    }

    public void processPoint(double x, double y, Set<String> terms) {
        for (String term : terms) {
            termFrequencies.put(term, termFrequencies.getOrDefault(term, 0) + 1);
            totalTermFrequency++;

            if (termFrequencies.get(term) >= epsilon * totalTermFrequency) {
                aspTrees.computeIfAbsent(term, k -> new ASPTree(space.minX, space.minY, space.maxX, space.maxY));
            }

            if (aspTrees.containsKey(term)) {
                aspTrees.get(term).put(x, y);
            }
        }
    }

    public double estimateSelectivity(Box queryRange, Set<String> queryTerms) {
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
}
