/**
 * Abhay Vivek Kulkarni
 * ak6277
 *
 * CSCI 654
 * Project 3
 */

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// For printing a visual tree shape
class BTreePrinter {
    /**
     * Code used for printing tree
     * Form StackOverFlow:
     *      https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram
     * User Answer:
     *      https://stackoverflow.com/a/4973083
     */
    public static void printNode(Node root) {
        int maxLevel = BTreePrinter.maxLevel(root);
        printNodeInternal(Collections.singletonList(root), 1, maxLevel);
    }

    private static void printNodeInternal(List<Node> nodes, int level, int maxLevel) {
        if (nodes.isEmpty() || BTreePrinter.isAllElementsNull(Collections.singletonList(nodes)))
            return;
        int floor = maxLevel - level;
        int endLines = (int) Math.pow(2, (Math.max(floor - 1, 0)));
        int firstSpaces = (int) Math.pow(2, (floor)) - 1;
        int betweenSpaces = (int) Math.pow(2, (floor + 1)) - 1;
        BTreePrinter.printWhitespaces(firstSpaces);
        List<Node> newNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (node != null) {
                System.out.print(node.keyLocation);
                newNodes.add(node.leftChild);
                newNodes.add(node.rightChild);
            } else {
                newNodes.add(null);
                newNodes.add(null);
                System.out.print(" ");
            }
            BTreePrinter.printWhitespaces(betweenSpaces);
        }
        System.out.println();
        for (int i = 1; i <= endLines; i++) {
            for (Node node : nodes) {
                BTreePrinter.printWhitespaces(firstSpaces - i);
                if (node == null) {
                    BTreePrinter.printWhitespaces(endLines + endLines + i + 1);
                    continue;
                }
                if (node.leftChild != null)
                    System.out.print("/");
                else
                    BTreePrinter.printWhitespaces(1);
                BTreePrinter.printWhitespaces(i + i - 1);
                if (node.rightChild != null)
                    System.out.print("\\");
                else
                    BTreePrinter.printWhitespaces(1);
                BTreePrinter.printWhitespaces(endLines + endLines - i);
            }
            System.out.println();
        }
        printNodeInternal(newNodes, level + 1, maxLevel);
    }

    private static void printWhitespaces(int count) {
        for (int i = 0; i < count; i++)
            System.out.print(" ");
    }

    private static int maxLevel(Node node) {
        if (node == null)
            return 0;
        return Math.max(BTreePrinter.maxLevel(node.leftChild), BTreePrinter.maxLevel(node.rightChild)) + 1;
    }

    private static boolean isAllElementsNull(List<Object> list) {
        for (Object object : list) {
            if (object != null)
                return false;
        }

        return true;
    }
}

// Node class
class Node {
    int keyLocation, start, end;
    Node leftChild = null, rightChild = null;

    Node(int keyLocation, int start, int end) {
        this.keyLocation = keyLocation;
        this.start = start;
        this.end = end;
    }
}

public class parallelOBST {
    static final int RANDOM_RANGE = 100; // 0-99
    static final boolean TEST = true; // true-> will perform tests
    static int NUM_OF_THREADS = 8; // Specify num of threads
    static final double ERROR = 1e-6; // Margin of error for double comparison
    static final boolean BUILD_TREE = false; // Builds a tree & prints it

    public static void main(String[] args) {
        int inputSize = 0;
        try {
            inputSize = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            System.exit(-10);
        }
        if (TEST)
            doTesting();
        // int[] threadCount = new int[]{1, 2, 4, 8, 12};
        // for (int N : threadCount) {
        // NUM_OF_THREADS = N;
        double[] accessProbabilities = generateNumbers(inputSize);
        long start_timer_all = System.nanoTime();
        double answer = findOptimalSolution(accessProbabilities);
        System.out.println("******************************************************************************");
        if (NUM_OF_THREADS > Runtime.getRuntime().availableProcessors())
            NUM_OF_THREADS = Runtime.getRuntime().availableProcessors();
        System.out.println("For input size of [" + inputSize + "] using [" + NUM_OF_THREADS + "] threads");
        System.out.println("Average number of key comparisons required for Optimal BST: \n\t\t" + answer);
        System.out.println("\nTime taken: " + (System.nanoTime() - start_timer_all) / 1e6 + " ms");
        System.out.println("******************************************************************************");

        // }

    }

    // Tests
    private static void doTesting() {
        System.out.println("******************************* CASE 1 **************************************");
        double[] trial = new double[]{4, 2, 6, 3};
        printKeyValue(trial);
        double ans1 = findOptimalSolution((trial));
        if (Math.abs(26 - ans1) < ERROR)
            System.out.println("Best Cost : " + ans1 + "\n");
        else
            System.err.println("Issue with --> test case :1.1");
        System.out.println("*******************************  CASE 1 with NORMALIZATION ***************************************");
        normalize(trial, 15);
        printKeyValue(trial);
        double ans2 = findOptimalSolution((trial));
        if (Math.abs(1.73333333 - ans2) < ERROR)
            System.out.println("Best Cost : " + ans2 + "\n");
        else
            System.err.println("Issue with --> test case :1.2");
        System.out.println("*****************************  CASE 2  ******************************************");
        trial = new double[]{0.213, 0.02, 0.547, 0.1, 0.12};
        printKeyValue(trial);
        double ans3 = findOptimalSolution((trial));
        if (Math.abs(1.573 - ans3) < ERROR)
            System.out.println("Best Cost :" + ans3 + "\n");
        else
            System.err.println("Issue with --> test case :2");
        System.out.println("******************************************************************************");
    }

    // Test-Printing
    private static void printKeyValue(double[] trial) {
        System.out.println();
        for (int num = 0; num < trial.length; num++)
            System.out.print("[k" + (num + 1) + ": " + trial[num] + "]\t");
        System.out.println();
    }

    // Main algorithm
    private static double findOptimalSolution(double[] keyProbabilityArray) {

        //***************************************************************************************
        int n = keyProbabilityArray.length;
        double[][] c = new double[n + 2][n + 1];
        //***************************************************************************************
        int[][] r = null;
        if (BUILD_TREE)
            r = new int[n + 2][n + 1];
        //***************************************************************************************
        // row 0 && last row is 0 padded
        // col 0 is 0 padded
        // i==j are initiated with their costs/probabilities
        //***************************************************************************************
        for (int row = 1; row < c.length; row++)
            for (int col = 0; col < c[0].length; col++) {
                if ((row == col) && (row < n + 1)) {
                    c[row][col] = keyProbabilityArray[row - 1];
                    if (BUILD_TREE)
                        r[row][col] = row;
                }
            }
        //***************************************************************************************
        // Updates values diagonal by diagonal
        //***************************************************************************************
        ForkJoinPool pool = null;
        try {
            pool = new ForkJoinPool(NUM_OF_THREADS);
            for (int diagonal = 1; diagonal < n; diagonal++) {
                //***************************************************************************************
                // Parallelize this for-loop
                //***************************************************************************************
                /*
                for (int i = 1; i <= (n - diagonal); i++) {
                    int j = diagonal + i;
                    double min = Double.MAX_VALUE;
                    for (int l = i; l <= j; l++) {
                        double q = c[i][l - 1] + c[l + 1][j];
                        min = Math.min(q, min);
                    }
                    c[i][j] = min + sumRange(i, j, keyProbabilityArray);
                }
                */
                //***************************************************************************************
                //  PARALLEL FOREACH STREAM IN FORK JOIN POOL
                //***************************************************************************************
                int finalDiagonal = diagonal;
                Consumer<Integer> consumer = i -> {
                    int j = finalDiagonal + i;
                    double min = Double.MAX_VALUE;
                    for (int l = i; l <= j; l++) {
                        double q = c[i][l - 1] + c[l + 1][j];
                        if (q < min) {
                            min = q;
                            // For back-tracking the roots that provided optimal answer
                            if (BUILD_TREE) {
                                r[i][j] = l;
                            }
                        }
                    }
                    c[i][j] = min + sumRange(i, j, keyProbabilityArray);
                };
                //***************************************************************************************
                Stream<Integer> numbers = IntStream.range(1, (n - diagonal + 1)).boxed();
                //***************************************************************************************
                pool.submit(() -> numbers.parallel().forEach(consumer)).get();
                //***************************************************************************************
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pool != null)
                pool.shutdown();
        }
        if (BUILD_TREE)
            buildATree(r, n);
        return c[1][n];
        //***************************************************************************************
    }

    // For building a tree if flag is set
    private static void buildATree(int[][] r, int n) {
        Node root = new Node(r[1][n], 1, n);
        Stack<Node> s = new Stack<>();
        s.push(root);
        while (!s.empty()) {
            Node currentNode = s.pop();
            int key = r[currentNode.start][currentNode.end];
            if (key != 0) {
                // Right side of tree
                if (key < currentNode.end) {
                    Node rChild = new Node(r[key + 1][currentNode.end], key + 1, currentNode.end);
                    currentNode.rightChild = rChild;
                    s.push(rChild);
                }
                if (currentNode.start < key) {
                    Node lChild = new Node(r[currentNode.start][key - 1], currentNode.start, key - 1);
                    currentNode.leftChild = lChild;
                    s.push(lChild);
                }
            }
        }
        BTreePrinter.printNode(root);
    }

    // For sum of frequencies/probabilities
    private static double sumRange(int i, int j, double[] trial) {
        double count = 0;
        for (int x = i - 1; x < j; x++)
            count += trial[x];
        return count;
    }

    // Random number generator
    private static double[] generateNumbers(int inputSize) {
        double[] result = new double[inputSize];
        long counter = 0;
        Random rand = new Random();
        for (int i = 0; i < inputSize; i++) {
            int temp = rand.nextInt(RANDOM_RANGE);
            counter += temp;
            result[i] = temp;
        }
        normalize(result, counter);
        return result;
    }

    // Normalizes the input
    private static void normalize(double[] result, long counter) {
        for (int i = 0; i < result.length; i++)
            result[i] /= counter;
    }
}
