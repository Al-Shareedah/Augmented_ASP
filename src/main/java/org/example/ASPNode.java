    package org.example;
    import util.Box;
    public class ASPNode {

    private boolean hasChildren = false;
    private ASPNode parent;
    private ASPNode NW = null;
    private ASPNode NE = null;
    private ASPNode SE = null;
    private ASPNode SW = null;
    // counter for all nodes in the tree
    private int Count = 0;
    //Split threshold
    private double alpha = 0.5;
    // Total size of all points inserted

    // Minimum resolution
    private static final double MIN_RESOLUTION = 1.0;


        private Box bounds;
        public ASPNode(double minX, double minY, double maxX, double maxY, ASPNode parent) {
            this.bounds = new Box(minX, minY, maxX, maxY);
            this.Count = 0;
            this.parent = parent;
        }
        public Box getBounds() {
            return this.bounds;
        }
        public int getCount() {
            return Count;
        }


        public boolean put(double x, double y, ASPTree tree) {
            if (!this.bounds.contains(x, y)) {
                return false;
            }
            if (this.hasChildren) {
                return getChild(x, y).put(x, y, tree);
            }

            double split_threshold = alpha * tree.getSizeN();
            if (this.Count >= split_threshold) {
                this.refine();
            }

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
        public void refine() {
            // Check if subdividing this node would result in a box smaller than the minimum resolution
            double halfWidth = (this.bounds.maxX - this.bounds.minX) / 2;
            double halfHeight = (this.bounds.maxY - this.bounds.minY) / 2;

            if (halfWidth < MIN_RESOLUTION || halfHeight < MIN_RESOLUTION) {
                // Do not subdivide further if either dimension of the new box would be below the minimum resolution
                return;
            }

            boolean hasExistingChildren = this.NW != null || this.NE != null || this.SE != null || this.SW != null;

            // Temporarily store existing children if any
            ASPNode[] existingChildren = {this.NW, this.NE, this.SE, this.SW};

            // Reset existing children references before potentially creating new children
            this.NW = this.NE = this.SE = this.SW = null;


            // Split the current node into four children
            this.NW = new ASPNode(this.bounds.minX, this.bounds.centreY, this.bounds.centreX, this.bounds.maxY, this);
            this.NE = new ASPNode(this.bounds.centreX, this.bounds.centreY, this.bounds.maxX, this.bounds.maxY, this);
            this.SE = new ASPNode(this.bounds.centreX, this.bounds.minY, this.bounds.maxX, this.bounds.centreY, this);
            this.SW = new ASPNode(this.bounds.minX, this.bounds.minY, this.bounds.centreX, this.bounds.centreY, this);
            this.hasChildren = true;

            if (hasExistingChildren) {
                // Check if all existing children fit within any one of the new children
                ASPNode commonNewParent = null;
                for (ASPNode potentialParent : new ASPNode[] {this.NW, this.NE, this.SE, this.SW}) {
                    boolean allFit = true;
                    for (ASPNode child : existingChildren) {
                        if (child != null && !child.getBounds().contains(potentialParent.getBounds())) {
                            allFit = false;
                            break;
                        }
                    }
                    if (allFit) {
                        commonNewParent = potentialParent;
                        break;
                    }
                }

                // If a common new parent is found, set it as the parent for all existing children
                if (commonNewParent != null) {
                    for (ASPNode child : existingChildren) {
                        if (child != null) {
                            child.setParent(commonNewParent);
                            // Depending on your implementation, you may also need to add the child
                            // to the commonNewParent's list of children here.
                        }
                    }
                }
            }

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
        public ASPNode getNodeContaining(double x, double y) {
            // If this node is a leaf or contains the point, return this node
            if (!this.hasChildren || this.bounds.contains(x, y)) {
                return this;
            }
            // Otherwise, traverse to the correct child node
            return getChild(x, y).getNodeContaining(x, y);
        }
        // Method to find the smallest (leaf) node containing a point
        public ASPNode getSmallestNodeContaining(double x, double y) {
            if (!this.hasChildren || !this.bounds.contains(x, y)) {
                return this;
            }
            for (ASPNode child : new ASPNode[] {NW, NE, SE, SW}) {
                if (child != null && child.bounds.contains(x, y)) {
                    return child.getSmallestNodeContaining(x, y);
                }
            }
            return this;
        }

        public ASPNode getNodeContaining(Box queryBox) {
            // Start from the smallest node containing the query box's minimum corner
            ASPNode smallestNode = getSmallestNodeContaining(queryBox.minX, queryBox.minY);

            // Traverse upward to find the node that fully encompasses the query box
            // but is not equal to the query box
            while (smallestNode != null) {
                if (smallestNode.bounds.contains(queryBox) && !smallestNode.bounds.equals(queryBox)) {
                    // If the current node encompasses the query box but is not equal to it
                    return smallestNode;
                }
                // Move to the parent node for further checking
                smallestNode = smallestNode.parent;
            }

            return null; // No suitable node found
        }
        public boolean isMergeable() {
            if (!this.hasChildren) {
                return false;
            }
            int leafChildrenCount = 0;
            if (this.NW != null && !this.NW.hasChildren) leafChildrenCount++;
            if (this.NE != null && !this.NE.hasChildren) leafChildrenCount++;
            if (this.SE != null && !this.SE.hasChildren) leafChildrenCount++;
            if (this.SW != null && !this.SW.hasChildren) leafChildrenCount++;

            return leafChildrenCount >= 3;
        }

        // New method to calculate the node's key
        public int getKey() {
            int childrenSum = 0;
            if (this.NW != null) childrenSum += this.NW.getCount();
            if (this.NE != null) childrenSum += this.NE.getCount();
            if (this.SE != null) childrenSum += this.SE.getCount();
            if (this.SW != null) childrenSum += this.SW.getCount();

            return this.getCount() + childrenSum;
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
        // New method to get the parent
        public ASPNode getParent() {
            return parent;
        }

        public void IncrementCounter() {
            Count++;
        }

        public void setCount(int count) {
            Count = count;
        }

        public void setNW(ASPNode NW) {
            this.NW = NW;
        }

        public void setNE(ASPNode NE) {
            this.NE = NE;
        }

        public void setSE(ASPNode SE) {
            this.SE = SE;
        }

        public void setSW(ASPNode SW) {
            this.SW = SW;
        }

        public void setParent(ASPNode parent) {
            this.parent = parent;
        }

        public void setHasChildren(boolean hasChildren) {
            this.hasChildren = hasChildren;
        }
    }