package com.aerospace.flightpath.cli;

import com.aerospace.flightpath.algorithm.*;
import com.aerospace.flightpath.data.FlightNetworkData;
import com.aerospace.flightpath.dsa.graph.Graph;
import com.aerospace.flightpath.model.*;

import java.util.*;

/**
 * Interactive Terminal / CLI Console Runner for the Aerospace Flight Path Optimizer.
 * Demonstrates graph search, heuristic comparisons, dynamic rerouting, and ASCII flight plans.
 */
public class CliRunner {

    public static void run(String[] args) {
        FlightNetworkData.NetworkBundle bundle = FlightNetworkData.createGlobalNetwork();
        Graph graph = bundle.graph;
        List<RestrictedZone> zones = bundle.restrictedZones;
        AircraftProfile aircraft = AircraftProfile.boeing787();

        printBanner();

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("Select an option [1-6]: ");
            String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "6";

            switch (input) {
                case "1":
                    findOptimalRouteInteractive(scanner, graph, zones, aircraft);
                    break;
                case "2":
                    runDsaBenchmarkComparison(graph, aircraft);
                    break;
                case "3":
                    compareMultiObjectiveOptimization(scanner, graph, zones, aircraft);
                    break;
                case "4":
                    simulateDynamicWaypointRerouting(scanner, graph, zones, aircraft);
                    break;
                case "5":
                    testYenKShortestAlternatives(scanner, graph, zones, aircraft);
                    break;
                case "6":
                    System.out.println("\nExiting AstraNav. Clear skies!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1-6.");
            }
        }
    }

    private static void printBanner() {
        System.out.println("\n===============================================================================");
        System.out.println("  ✈️  ASTRANAV — AEROSPACE NAVIGATION & PATHFINDING ENGINE (JAVA DSA)");
        System.out.println("  Algorithms: Dijkstra, A*, BFS, DFS, Yen's K-Shortest, Ray-Casting Avoidance");
        System.out.println("===============================================================================");
    }

    private static void printMenu() {
        System.out.println("\n-------------------------------------------------------------------------------");
        System.out.println(" 1. Calculate Optimal Route (Dijkstra vs A*)");
        System.out.println(" 2. Run DSA Benchmark: Dijkstra vs A* vs BFS (Nodes Explored & Time)");
        System.out.println(" 3. Compare Multi-Objective Routes (Shortest Distance vs Fastest vs Fuel)");
        System.out.println(" 4. Simulate Dynamic Contingency: Waypoint Failure & Storm Detour Rerouting");
        System.out.println(" 5. Generate Top-K Alternative Flight Routes (Yen's Algorithm)");
        System.out.println(" 6. Exit");
        System.out.println("-------------------------------------------------------------------------------");
    }

    private static void findOptimalRouteInteractive(Scanner scanner, Graph graph, List<RestrictedZone> zones, AircraftProfile aircraft) {
        System.out.print("\nEnter Origin Airport Code [e.g. JFK, SFO, LHR, DXB] (default JFK): ");
        String orig = scanner.nextLine().trim().toUpperCase();
        if (orig.isEmpty()) orig = "JFK";

        System.out.print("Enter Destination Airport Code [e.g. LHR, SFO, CDG, DXB] (default LHR): ");
        String dest = scanner.nextLine().trim().toUpperCase();
        if (dest.isEmpty()) dest = "LHR";

        Node originNode = graph.getNode(orig);
        Node destNode = graph.getNode(dest);

        if (originNode == null || destNode == null) {
            System.out.println("Error: Unknown airport code(s). Available: JFK, LHR, CDG, FRA, ORD, SFO, LAX, DXB, SIN, etc.");
            return;
        }

        System.out.println("\n--- Computing Optimal Flight Route with A* Search ---");
        RouteResult aStarResult = AStarRouter.findRoute(graph, originNode, destNode, OptimizationObjective.DISTANCE, aircraft, zones);
        printRouteResult(aStarResult);

        System.out.println("\n--- Computing Optimal Flight Route with Dijkstra's Algorithm ---");
        RouteResult dijkstraResult = DijkstraRouter.findRoute(graph, originNode, destNode, OptimizationObjective.DISTANCE, aircraft, zones);
        printRouteResult(dijkstraResult);

        System.out.printf(">> Comparison: A* visited %d nodes, Dijkstra visited %d nodes (Efficiency Gain: %.1f%% fewer nodes)\n",
                aStarResult.getNodesExplored(), dijkstraResult.getNodesExplored(),
                ((dijkstraResult.getNodesExplored() - aStarResult.getNodesExplored()) / (double) dijkstraResult.getNodesExplored()) * 100.0);
    }

    private static void runDsaBenchmarkComparison(Graph graph, AircraftProfile aircraft) {
        System.out.println("\n===============================================================================");
        System.out.println("                    DSA BENCHMARK: A* vs DIJKSTRA vs BFS                       ");
        System.out.println("===============================================================================");
        System.out.printf("%-26s | %-16s | %-16s | %-16s\n", "Route Test Case", "A* (Nodes / µs)", "Dijkstra (Nodes / µs)", "BFS (Nodes / µs)");
        System.out.println("-------------------------------------------------------------------------------");

        String[][] testPairs = {
                {"JFK", "LHR", "North Atlantic Track"},
                {"JFK", "SFO", "US Transcontinental"},
                {"LHR", "DXB", "Europe -> Middle East"},
                {"ORD", "LAX", "Midwest to West Coast"}
        };

        for (String[] pair : testPairs) {
            Node u = graph.getNode(pair[0]);
            Node v = graph.getNode(pair[1]);

            RouteResult aStar = AStarRouter.findRoute(graph, u, v, OptimizationObjective.DISTANCE, aircraft, null);
            RouteResult dijkstra = DijkstraRouter.findRoute(graph, u, v, OptimizationObjective.DISTANCE, aircraft, null);
            RouteResult bfs = BfsDfsRouter.findShortestHopsRoute(graph, u, v, aircraft, null);

            String testLabel = pair[0] + " -> " + pair[1];
            String aStr = String.format("%d nodes / %d µs", aStar.getNodesExplored(), aStar.getExecutionTimeMicros());
            String dStr = String.format("%d nodes / %d µs", dijkstra.getNodesExplored(), dijkstra.getExecutionTimeMicros());
            String bStr = String.format("%d nodes / %d µs", bfs.getNodesExplored(), bfs.getExecutionTimeMicros());

            System.out.printf("%-26s | %-16s | %-16s | %-16s\n", testLabel, aStr, dStr, bStr);
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("DSA Observation: A* with Haversine heuristic drastically prunes search space,");
        System.out.println("exploring only nodes directed along the great-circle geodesic corridor!");
    }

    private static void compareMultiObjectiveOptimization(Scanner scanner, Graph graph, List<RestrictedZone> zones, AircraftProfile aircraft) {
        System.out.print("\nEnter Origin (default JFK): ");
        String orig = scanner.nextLine().trim().toUpperCase();
        if (orig.isEmpty()) orig = "JFK";
        System.out.print("Enter Destination (default LHR): ");
        String dest = scanner.nextLine().trim().toUpperCase();
        if (dest.isEmpty()) dest = "LHR";

        Node u = graph.getNode(orig);
        Node v = graph.getNode(dest);
        if (u == null || v == null) return;

        RouteResult distRoute = DijkstraRouter.findRoute(graph, u, v, OptimizationObjective.DISTANCE, aircraft, zones);
        RouteResult timeRoute = DijkstraRouter.findRoute(graph, u, v, OptimizationObjective.TIME, aircraft, zones);
        RouteResult fuelRoute = DijkstraRouter.findRoute(graph, u, v, OptimizationObjective.FUEL, aircraft, zones);

        System.out.println("\n=== MULTI-OBJECTIVE OPTIMIZATION COMPARISON (" + orig + " -> " + dest + ") ===");
        System.out.printf("%-20s | %-14s | %-14s | %-14s\n", "Objective", "Distance (km)", "Flight Time", "Fuel Burn (kg)");
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("%-20s | %-14.1f | %-14s | %-14.1f\n", "1. Shortest Distance", distRoute.getTotalDistanceKm(), distRoute.getFormattedFlightTime(), distRoute.getTotalFuelKg());
        System.out.printf("%-20s | %-14.1f | %-14s | %-14.1f\n", "2. Fastest (Wind-Opt)", timeRoute.getTotalDistanceKm(), timeRoute.getFormattedFlightTime(), timeRoute.getTotalFuelKg());
        System.out.printf("%-20s | %-14.1f | %-14s | %-14.1f\n", "3. Lowest Fuel Burn", fuelRoute.getTotalDistanceKm(), fuelRoute.getFormattedFlightTime(), fuelRoute.getTotalFuelKg());
        System.out.println("-------------------------------------------------------------------------------");
    }

    private static void simulateDynamicWaypointRerouting(Scanner scanner, Graph graph, List<RestrictedZone> zones, AircraftProfile aircraft) {
        System.out.println("\nSimulating Contingency on JFK -> LHR route (North Atlantic):");
        System.out.println("Imagine oceanic waypoint '54N40W' becomes unavailable due to a severe storm cell.");

        Node jfk = graph.getNode("JFK");
        Node lhr = graph.getNode("LHR");

        Set<String> disabled = new HashSet<>(Collections.singletonList("54N40W"));
        DynamicRerouter.RerouteAnalysis analysis = DynamicRerouter.planDetour(
                graph, jfk, lhr, OptimizationObjective.DISTANCE, aircraft, disabled, zones);

        System.out.println(analysis.toString());
    }

    private static void testYenKShortestAlternatives(Scanner scanner, Graph graph, List<RestrictedZone> zones, AircraftProfile aircraft) {
        System.out.print("\nEnter Origin (default JFK): ");
        String orig = scanner.nextLine().trim().toUpperCase();
        if (orig.isEmpty()) orig = "JFK";
        System.out.print("Enter Destination (default LHR): ");
        String dest = scanner.nextLine().trim().toUpperCase();
        if (dest.isEmpty()) dest = "LHR";

        Node u = graph.getNode(orig);
        Node v = graph.getNode(dest);
        if (u == null || v == null) return;

        System.out.println("\n--- Calculating Top 4 Alternative Routes using Yen's Algorithm ---");
        List<RouteResult> kPaths = YenKShortestPaths.findKShortestPaths(graph, u, v, OptimizationObjective.DISTANCE, aircraft, zones, 4);

        for (int i = 0; i < kPaths.size(); i++) {
            RouteResult r = kPaths.get(i);
            System.out.printf("\n[Option #%d] %s: Distance=%.1f km, Time=%s, Fuel=%.1f kg\n",
                    i + 1, r.getAlgorithmName(), r.getTotalDistanceKm(), r.getFormattedFlightTime(), r.getTotalFuelKg());
            System.out.print("   Path: ");
            for (int p = 0; p < r.getPath().size(); p++) {
                System.out.print(r.getPath().get(p).getId());
                if (p < r.getPath().size() - 1) System.out.print(" -> ");
            }
            System.out.println();
        }
    }

    private static void printRouteResult(RouteResult res) {
        System.out.print(res.toString());
    }
}
