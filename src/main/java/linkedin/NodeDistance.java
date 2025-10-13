package linkedin;

// Java Program to Find distance between two
// nodes of a Binary Tree

class Node {
    public int data;
    public Node left, right;

    Node(int val) {
        data = val;
        left = null;
        right = null;
    }
}

class GfG {

    // Function to find the level of a node
    static int findLevel(Node root, int k, int level) {
        if (root == null) return -1;
        if (root.data == k) return level;

        // Recursively call function on left child
        int leftLevel = findLevel(root.left, k, level + 1);

        // If node is found on left, return level
        // Else continue searching on the right child
        if (leftLevel != -1) {
            return leftLevel;
        } else {
            return findLevel(root.right, k, level + 1);
        }
    }

    // Function to find the lowest common ancestor
    // and calculate distance between two nodes
    static Node findLcaAndDistance(Node root, int a, int b,
                                   int[] d1, int[] d2, int[] dist, int lvl) {
        if (root == null) return null;

        if (root.data == a) {

            // If first node found, store level and
            // return the node
            d1[0] = lvl;
            return root;
        }
        if (root.data == b) {

            // If second node found, store level and
            // return the node
            d2[0] = lvl;
            return root;
        }

        // Recursively call function on left child
        Node left = findLcaAndDistance
                (root.left, a, b, d1, d2, dist, lvl + 1);

        // Recursively call function on right child
        Node right = findLcaAndDistance
                (root.right, a, b, d1, d2, dist, lvl + 1);

        if (left != null && right != null) {

            // If both nodes are found in different
            // subtrees, calculate the distance
            dist[0] = d1[0] + d2[0] - 2 * lvl;
        }

        // Return node found or null if not found
        if (left != null) {
            return left;
        } else {
            return right;
        }
    }

    // Function to find distance between two nodes
    static int findDist(Node root, int a, int b) {
        int[] d1 = {-1}, d2 = {-1}, dist = {0};

        // Find lowest common ancestor and calculate distance
        Node lca = findLcaAndDistance(root, a, b, d1, d2, dist, 1);

        if (d1[0] != -1 && d2[0] != -1) {

            // Return the distance if both nodes are found
            return dist[0];
        }

        if (d1[0] != -1) {

            // If only first node is found, find
            // distance to second node
            dist[0] = findLevel(lca, b, 0);
            return dist[0];
        }

        if (d2[0] != -1) {

            // If only second node is found, find
            // distance to first node
            dist[0] = findLevel(lca, a, 0);
            return dist[0];
        }

        // Return -1 if both nodes not found
        return -1;
    }

    public static void main(String[] args) {

        // Hardcoded binary tree
        //        1
        //      /   \
        //     2     3
        //    / \   / \
        //   4   5 6   7

        Node root = new Node(1);
        root.left = new Node(2);
        root.right = new Node(3);
        root.left.left = new Node(4);
        root.left.right = new Node(5);
        root.right.left = new Node(6);
        root.right.right = new Node(7);

        int a = 4, b = 7;
        System.out.println(findDist(root, a, b));
    }
}