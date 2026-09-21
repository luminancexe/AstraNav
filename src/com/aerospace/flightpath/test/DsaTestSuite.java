package com.aerospace.flightpath.test;

import com.aerospace.flightpath.algorithm.*;
import com.aerospace.flightpath.data.FlightNetworkData;
import com.aerospace.flightpath.dsa.geometry.AirspaceGeometry;
import com.aerospace.flightpath.dsa.graph.AdjacencyListGraph;
import com.aerospace.flightpath.dsa.graph.AdjacencyMatrixGraph;
import com.aerospace.flightpath.dsa.graph.Graph;
import com.aerospace.flightpath.dsa.heap.IndexedMinHeap;
import com.aerospace.flightpath.dsa.heap.MinHeap;
import com.aerospace.flightpath.dsa.sort.RouteSorter;
import com.aerospace.flightpath.model.*;

import java.util.*;

/**
 * Self-contained automated test suite for all aerospace DSA concepts:
 * - MinHeap & IndexedMinHeap
 * - Graph Adjacency List & Matrix
 * - Dijkstra vs A* Optimality & Heuristic Verification
 * - BFS Min Hops & DFS Traversal
 * - Yen's K-Shortest Paths
 * - Airspace Geometry & Collision Detection
 * - QuickSort & MergeSort
 */
public class DsaTestSuite {
    private static int totalPassed = 0;
    private static int totalFailed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println(" 🧪 RUNNING ASTRANAV DSA TEST SUITE ");
        System.out.println("==================================================================");

        testMinHeap();
        testIndexedMinHeap();
        testGraphParity();
        testDijkstraAndAStarParity();
        testBfsAndDfs();
        testYenKShortestPaths();
        testGeometryCollision();
        testSortingAlgorithms();

        System.out.println("==================================================================");
        System.out.printf(" RESULTS: %d PASSED, %d FAILED\n", totalPassed, totalFailed);
        System.out.println("==================================================================");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            System.out.printf("  [PASS] %s\n", testName);
            totalPassed++;
        } else {
            System.err.printf("  [FAIL] %s\n", testName);
            totalFailed++;
        }
    }

    private static void assertEquals(double expected, double actual, double epsilon, String testName) {
        assertTrue(Math.abs(expected - actual) <= epsilon,
                String.format("%s (Expected: %.4f, Actual: %.4f)", testName, expected, actual));
    }

    private static void testMinHeap() {
        System.out.println("\n--- Testing Custom MinHeap ---");
        MinHeap<Integer> heap = new MinHeap<>();
        assertTrue(heap.isEmpty(), "MinHeap starts empty");

        int[] inputs = {42, 10, 89, 5, 23, 1, 99, 14};
        for (int v : inputs) heap.insert(v);

        assertTrue(heap.size() == inputs.length, "MinHeap size matches inserted count");
        assertTrue(heap.peek() == 1, "MinHeap peek returns minimum element (1)");

        List<Integer> extracted = new ArrayList<>();
        while (!heap.isEmpty()) {
            extracted.add(heap.extractMin());
        }

        boolean sorted = true;
        for (int i = 0; i < extracted.size() - 1; i++) {
            if (extracted.get(i) > extracted.get(i + 1)) sorted = false;
        }
        assertTrue(sorted, "MinHeap extracts elements in strictly ascending sorted order");
    }

    private static void testIndexedMinHeap() {
        System.out.println("\n--- Testing IndexedMinHeap (decreaseKey) ---");
        IndexedMinHeap<String> heap = new IndexedMinHeap<>();
        heap.insertOrDecrease("JFK", 100.0);
        heap.insertOrDecrease("LHR", 50.0);
        heap.insertOrDecrease("CDG", 75.0);

        assertTrue(heap.contains("LHR"), "Contains key LHR");
        assertTrue(heap.getPriority("LHR") == 50.0, "Priority for LHR is 50.0");

        // Decrease JFK priority from 100.0 down to 20.0
        heap.insertOrDecrease("JFK", 20.0);
        assertTrue(heap.extractMin().equals("JFK"), "JFK decreased to 20.0 and became new minimum root");
        assertTrue(heap.extractMin().equals("LHR"), "LHR is next minimum root (50.0)");
        assertTrue(heap.extractMin().equals("CDG"), "CDG is final root (75.0)");
    }

    private static void testGraphParity() {
        System.out.println("\n--- Testing Graph Parity (Adjacency List vs Adjacency Matrix) ---");
        FlightNetworkData.NetworkBundle bundle = FlightNetworkData.createGlobalNetwork();
        Graph adjListGraph = bundle.graph;
        Graph matrixGraph = AdjacencyMatrixGraph.fromGraph(adjListGraph);

        assertTrue(adjListGraph.getNodeCount() == matrixGraph.getNodeCount(),
                "Node counts match between List and Matrix (" + adjListGraph.getNodeCount() + ")");
        assertTrue(adjListGraph.getEdgeCount() == matrixGraph.getEdgeCount(),
                "Edge counts match between List and Matrix (" + adjListGraph.getEdgeCount() + ")");

        Node jfk = adjListGraph.getNode("JFK");
        Node bos = adjListGraph.getNode("BOS");
        assertTrue(adjListGraph.hasEdge(jfk, bos) == matrixGraph.hasEdge(jfk, bos),
                "Direct edge query (JFK -> BOS) identical in List and Matrix");
    }

    private static void testDijkstraAndAStarParity() {
        System.out.println("\n--- Testing Dijkstra vs A* Optimality & Heuristic Verification ---");
        FlightNetworkData.NetworkBundle bundle = FlightNetworkData.createGlobalNetwork();
        Graph graph = bundle.graph;
        AircraftProfile ac = AircraftProfile.boeing787();

        Node jfk = graph.getNode("JFK");
        Node lhr = graph.getNode("LHR");

        RouteResult dijkstra = DijkstraRouter.findRoute(graph, jfk, lhr, OptimizationObjective.DISTANCE, ac, null);
        RouteResult aStar = AStarRouter.findRoute(graph, jfk, lhr, OptimizationObjective.DISTANCE, ac, null);

        assertTrue(dijkstra.isSuccess(), "Dijkstra finds path JFK -> LHR");
        assertTrue(aStar.isSuccess(), "A* finds path JFK -> LHR");

        assertEquals(dijkstra.getTotalDistanceKm(), aStar.getTotalDistanceKm(), 0.01,
                "A* and Dijkstra compute the EXACT same optimal geodesic distance");

        assertTrue(aStar.getNodesExplored() <= dijkstra.getNodesExplored(),
                "A* explores fewer or equal nodes than Dijkstra (A*: " + aStar.getNodesExplored() +
                        " vs Dijkstra: " + dijkstra.getNodesExplored() + ")");
    }

    private static void testBfsAndDfs() {
        System.out.println("\n--- Testing BFS & DFS Graph Traversals ---");
        FlightNetworkData.NetworkBundle bundle = FlightNetworkData.createGlobalNetwork();
        Graph graph = bundle.graph;
        AircraftProfile ac = AircraftProfile.boeing787();

        Node jfk = graph.getNode("JFK");
        Node lhr = graph.getNode("LHR");

        RouteResult bfsRoute = BfsDfsRouter.findShortestHopsRoute(graph, jfk, lhr, ac, null);
        assertTrue(bfsRoute.isSuccess(), "BFS finds path with minimum hops");
        assertTrue(bfsRoute.getHopCount() > 0, "BFS hop count is positive (" + bfsRoute.getHopCount() + " hops)");

        List<RouteResult> dfsRoutes = BfsDfsRouter.findAllPathsDfs(graph, jfk, lhr, ac, null, 10, 4);
        assertTrue(!dfsRoutes.isEmpty(), "DFS finds at least one viable path via backtracking");
        assertTrue(dfsRoutes.size() <= 4, "DFS respects maxPaths constraint (found: " + dfsRoutes.size() + ")");

        boolean cycleDetected = BfsDfsRouter.hasCycle(graph);
        assertTrue(cycleDetected, "Cycle detection correctly identifies loops in bidirectional network");
    }

    private static void testYenKShortestPaths() {
        System.out.println("\n--- Testing Yen's K-Shortest Paths Algorithm ---");
        FlightNetworkData.NetworkBundle bundle = FlightNetworkData.createGlobalNetwork();
        Graph graph = bundle.graph;
        AircraftProfile ac = AircraftProfile.boeing787();

        Node jfk = graph.getNode("JFK");
        Node lhr = graph.getNode("LHR");

        List<RouteResult> kPaths = YenKShortestPaths.findKShortestPaths(
                graph, jfk, lhr, OptimizationObjective.DISTANCE, ac, null, 3);

        assertTrue(kPaths.size() >= 2, "Yen's algorithm finds multiple alternate routes (found: " + kPaths.size() + ")");

        boolean monotonic = true;
        for (int i = 0; i < kPaths.size() - 1; i++) {
            if (kPaths.get(i).getTotalDistanceKm() > kPaths.get(i + 1).getTotalDistanceKm()) {
                monotonic = false;
            }
        }
        assertTrue(monotonic, "Alternative paths are monotonically ordered by cost (Rank 1 <= Rank 2 <= Rank 3)");
    }

    private static void testGeometryCollision() {
        System.out.println("\n--- Testing Computational Geometry & Restricted Airspace Avoidance ---");
        // Center: (54.0, -40.0), Radius: 500 km
        Coordinate stormCenter = new Coordinate(54.0, -40.0);
        Coordinate pointInside = new Coordinate(54.1, -40.1);
        Coordinate pointOutside = new Coordinate(20.0, 0.0);

        assertTrue(pointInside.haversineDistanceKm(stormCenter) < 500.0, "Point inside circular storm detected");
        assertTrue(pointOutside.haversineDistanceKm(stormCenter) > 500.0, "Point outside circular storm detected");

        // Segment directly passing through storm center: (54.0, -45.0) -> (54.0, -35.0)
        boolean intersects = RestrictedZone.segmentIntersectsCircle(
                new Coordinate(54.0, -45.0), new Coordinate(54.0, -35.0), stormCenter, 500.0);
        assertTrue(intersects, "Line segment intersecting storm cell correctly identified");

        // Segment far away: (20.0, -20.0) -> (21.0, -21.0)
        boolean farSegment = RestrictedZone.segmentIntersectsCircle(
                new Coordinate(20.0, -20.0), new Coordinate(21.0, -21.0), stormCenter, 500.0);
        assertTrue(!farSegment, "Distant line segment correctly determined to NOT intersect storm cell");
    }

    private static void testSortingAlgorithms() {
        System.out.println("\n--- Testing QuickSort & MergeSort Route Sorters ---");
        List<Integer> list1 = new ArrayList<>(Arrays.asList(99, 12, 45, 1, 87, 23, 6, 34));
        List<Integer> list2 = new ArrayList<>(list1);

        RouteSorter.quickSort(list1, Integer::compareTo);
        List<Integer> sortedMerge = RouteSorter.mergeSort(list2, Integer::compareTo);

        List<Integer> expected = Arrays.asList(1, 6, 12, 23, 34, 45, 87, 99);
        assertTrue(list1.equals(expected), "QuickSort correctly sorts list in-place");
        assertTrue(sortedMerge.equals(expected), "MergeSort correctly returns sorted list");
    }
}
