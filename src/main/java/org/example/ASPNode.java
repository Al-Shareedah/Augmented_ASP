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
    private static final double alpha = 0.5;
    // Total size of all points inserted
    private static int size_n = 0;

    private Box bounds;
        public ASPNode(double minX, double minY, double maxX, double maxY) {
            this.bounds = new Box(minX, minY, maxX, maxY);
            this.Count = 0;

        }
        public Box getBounds() {
            return this.bounds;
        }
        public int getCount() {
            return Count;
        }


        public boolean put(double x, double y) {
            if (!this.bounds.contains(x, y)) {
                return false;
            }
            if (this.hasChildren) {
                return getChild(x, y).put(x, y);
            }
            size_n++; // Increment the total size of points
            double split_threshold = alpha * size_n;
            if (this.Count >= split_threshold) {
                this.refine();
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
        private void refine() {
            // Split the current node into four children
            this.NW = new ASPNode(this.bounds.minX, this.bounds.centreY, this.bounds.centreX, this.bounds.maxY);
            this.NE = new ASPNode(this.bounds.centreX, this.bounds.centreY, this.bounds.maxX, this.bounds.maxY);
            this.SE = new ASPNode(this.bounds.centreX, this.bounds.minY, this.bounds.maxX, this.bounds.centreY);
            this.SW = new ASPNode(this.bounds.minX, this.bounds.minY, this.bounds.centreX, this.bounds.centreY);
            this.hasChildren = true;
        }
        /**
         * Calculates the size of the subtree rooted at this node.
         * @return the size of the subtree.
         */
        public int subtreeSize() {
            if (!hasChildren) {
                // If the node has no children, its size is 1 (itself).
                return 1;
            } else {
                // If the node has children, calculate the size recursively.
                int size = 1; // Start with 1 for the current node
                if (NW != null) size += NW.subtreeSize();
                if (NE != null) size += NE.subtreeSize();
                if (SE != null) size += SE.subtreeSize();
                if (SW != null) size += SW.subtreeSize();
                return size;
            }
        }

        public ASPNode getNW() {
            return NW;
        }

        public ASPNode getNE() {
            return NE;
        }

        public ASPNode getSE() {
            return SE;
        }

        public ASPNode getSW() {
            return SW;
        }

        public boolean isHasChildren() {
            return hasChildren;
        }
    }