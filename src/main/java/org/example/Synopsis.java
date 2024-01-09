package org.example;

import com.google.common.hash.HashFunction;

import java.util.*;

public class Synopsis {
    private static final double MIN_X = -180.0;
    private static final double MIN_Y = -90.0;
    private static final double MAX_X = 180.0;
    private static final double MAX_Y = 90.0;
    private Set<StreamingObject> objects;
    private HashFunction hashFunction;
    private TreeMap<Long, SortedSet<StreamingObject>> gridIndex;

    public Synopsis() {
        this.gridIndex = new TreeMap<>();

    }
    public void addObject(StreamingObject obj) {
        long zOrder = computeZOrder(obj.getX(), obj.getY());
        gridIndex.computeIfAbsent(zOrder, k -> new TreeSet<>(new ZOrderComparator()))
                .add(obj);
    }

    public void removeObject(StreamingObject obj) {
        long zOrder = computeZOrder(obj.getX(), obj.getY());
        if(gridIndex.containsKey(zOrder)) {
            SortedSet<StreamingObject> set = gridIndex.get(zOrder);
            set.remove(obj);
            if(set.isEmpty()) {
                gridIndex.remove(zOrder);
            }
        }
    }
    public boolean isEmpty() {
        return gridIndex.isEmpty();
    }

    // Retrieve objects by Z-order value
    public SortedSet<StreamingObject> getObjectsByZOrder(long zOrder) {
        return gridIndex.getOrDefault(zOrder, new TreeSet<>(new ZOrderComparator()));
    }

    // Method to compute Z-order given x, y coordinates
    static public long computeZOrder(double x, double y) {
        // Normalize the coordinates to [0, 1]
        double normalizedX = (x - MIN_X) / (MAX_X - MIN_X);
        double normalizedY = (y - MIN_Y) / (MAX_Y - MIN_Y);

        // Scale normalized coordinates to the range of positive long values
        long xNorm = (long)(normalizedX * Long.MAX_VALUE);
        long yNorm = (long)(normalizedY * Long.MAX_VALUE);

        // Interleave the bits of xNorm and yNorm
        long zOrder = 0;
        for (int i = 0; i < Long.SIZE / 2; i++) {
            zOrder |= (xNorm & (1L << i)) << i | (yNorm & (1L << i)) << (i + 1);
        }
        return zOrder;
    }

    public TreeMap<Long, SortedSet<StreamingObject>> getGridIndex() {
        return gridIndex;
    }

    class ZOrderComparator implements Comparator<StreamingObject> {
        @Override
        public int compare(StreamingObject o1, StreamingObject o2) {
            long z1 = o1.getZOrder();
            long z2 = o2.getZOrder();
            return Long.compare(z1, z2);
        }
    }



}
