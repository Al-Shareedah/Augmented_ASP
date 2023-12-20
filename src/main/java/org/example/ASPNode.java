    package org.example;
    import java.util.ArrayList;
    import java.util.Map;

    import util.AbstractDouble;
    import util.AbstractLeaf;
    import util.Box;
    public class ASPNode {

    private boolean hasChildren = false;
    private ASPNode NW = null;
    private ASPNode NE = null;
    private ASPNode SE = null;
    private ASPNode SW = null;
    // counter for all nodes in the tree
    private int Count = 0;

    private Box bounds;
        public ASPNode(double minX, double minY, double maxX, double maxY) {
            this.bounds = new Box(minX, minY, maxX, maxY);
            this.Count = 0;

        }
        public Box getBounds() {
            return this.bounds;
        }
        public int getLeafCount() {
            return Count;
        }


        public boolean put(double x, double y) {
            if (!this.bounds.contains(x, y)) {
                return false;
            }
            if (this.hasChildren) {
                return getChild(x, y).put(x, y);
            }
            // Increment the counter for the node that contains the point
            this.Count++;
            return true;
        }

        private ASPNode getChild(double x, double y) {
            if (this.hasChildren) {
                if (x < this.bounds.centreX) {
                    if (y < this.bounds.centreY)
                        return this.SW;
                    return this.NW;
                }
                if (y < this.bounds.centreY)
                    return this.SE;
                return this.NE;
            }
            return null;
        }
        private void divide() {
            this.NW = new ASPNode(this.bounds.minX, this.bounds.centreY, this.bounds.centreX, this.bounds.maxY);
            this.NE = new ASPNode(this.bounds.centreX, this.bounds.centreY, this.bounds.maxX, this.bounds.maxY);
            this.SE = new ASPNode(this.bounds.centreX, this.bounds.minY, this.bounds.maxX, this.bounds.centreY);
            this.SW = new ASPNode(this.bounds.minX, this.bounds.minY, this.bounds.centreX, this.bounds.centreY);
            this.hasChildren = true;

        }


    }