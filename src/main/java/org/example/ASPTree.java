package org.example;
import util.AbstractDouble;
import util.Box;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;

public class ASPTree {
    protected ASPNode root = null;
    private int size_n = 0;
    /**
     * Creates an empty ASPTree with the bounds
     */
    public ASPTree(double minX, double minY, double maxX, double maxY){
        this.root = new ASPNode(minX, minY, maxX, maxY);
    }
    public boolean put(double x, double y) {
        if (this.root.put(x, y)) {
            increaseSize();
            return true;
        }
        return false;
    }
    private void increaseSize() {
        this.size_n++;
    }




}

