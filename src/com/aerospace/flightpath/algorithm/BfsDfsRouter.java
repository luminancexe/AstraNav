package com.aerospace.flightpath.algorithm;

import com.aerospace.flightpath.dsa.graph.Graph;
import com.aerospace.flightpath.model.*;

import java.util.*;

/**
 * Fundamental Graph Traversal Algorithms:
 * - BFS: Minimum Waypoint Hops Route & Reachability
 * - DFS: Path Backtracking Enumeration & Airway Cycle Detection
 */
public class BfsDfsRouter {

    /**
     * Breadth-First Search (BFS) to find the route with the minimum number of airway hops / waypoints.
     * Time Complexity: O(V + E)
     * Space Complexity: O(V)
     */
    public static RouteResult findShortestHopsRoute(Graph graph, Node origin, Node destination,
                                                   AircraftProfile aircraft, List<RestrictedZone> restrictedZones) {
        long startTime = System.nanoTime();

        if (graph == null || origin == null || destination == null) {
            return RouteResult.failure("BFS (Min Hops)", OptimizationObjective.DISTANCE, aircraft,
                    "Origin or destination is null", 0, 0, null);
        }

        if (origin.equals(destination)) {
            return new RouteResult("BFS (Min Hops)", OptimizationObjective.DISTANCE, aircraft,
                    List.of(origin), Collections.emptyList(), 0, 0, 0, 1, 0, true, "Already at destination", List.of(origin.getId()));
        }

        Queue<Node> queue = new ArrayDeque<>();
        Map<Node, Node> parentMap = new HashMap<>();
        Map<Node, FlightSegment> edgeTo = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        List<String> exploredOrder = new ArrayList<>();

        queue.add(origin);
        visited.add(origin);
        exploredOrder.add(origin.getId());

        boolean found = false;

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current.equals(destination)) {
                found = true;
                break;
            }

            for (FlightSegment segment : graph.getOutgoingSegments(current)) {
                Node neighbor = segment.getTarget();

                if (!segment.isActive() || !neighbor.isActive()) {
                    continue;
                }

                if (restrictedZones != null && isSegmentRestricted(segment, aircraft, restrictedZones)) {
                    continue;
                }

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    exploredOrder.add(neighbor.getId());
                    parentMap.put(neighbor, current);
                    edgeTo.put(neighbor, segment);
                    queue.add(neighbor);

                    if (neighbor.equals(destination)) {
                        found = true;
                        break;
                    }
                }
            }

            if (found) break;
        }

        long executionTimeMicros = (System.nanoTime() - startTime) / 1000;

        if (!found) {
            return RouteResult.failure("BFS (Min Hops)", OptimizationObjective.DISTANCE, aircraft,
                    "No connected route found via BFS", visited.size(), executionTimeMicros, exploredOrder);
        }

        // Reconstruct path
        List<Node> path = new ArrayList<>();
        List<FlightSegment> segments = new ArrayList<>();
        Node curr = destination;

        while (curr != null) {
            path.add(curr);
            FlightSegment seg = edgeTo.get(curr);
            if (seg != null) segments.add(seg);
            curr = parentMap.get(curr);
        }

        Collections.reverse(path);
        Collections.reverse(segments);

        double totalDist = 0.0;
        double totalTime = 0.0;
        double totalFuel = 0.0;

        for (FlightSegment seg : segments) {
            totalDist += seg.getDistanceKm();
            totalTime += seg.getFlightTimeHours(aircraft);
            totalFuel += seg.getFuelConsumptionKg(aircraft);
        }

        return new RouteResult("BFS (Min Hops)", OptimizationObjective.DISTANCE, aircraft,
                path, segments, totalDist, totalTime, totalFuel, visited.size(), executionTimeMicros,
                true, "Path with minimum waypoint hops found", exploredOrder);
    }

    /**
     * Depth-First Search (DFS) with Backtracking to find all viable alternative paths
     * up to maxDepth hops, limited to maxPaths solutions.
     */
    public static List<RouteResult> findAllPathsDfs(Graph graph, Node origin, Node destination,
                                                   AircraftProfile aircraft, List<RestrictedZone> restrictedZones,
                                                   int maxDepth, int maxPaths) {
        long startTime = System.nanoTime();
        List<RouteResult> results = new ArrayList<>();
        Set<Node> currentPathSet = new HashSet<>();
        List<Node> currentPath = new ArrayList<>();
        List<FlightSegment> currentSegments = new ArrayList<>();
        int[] nodesExploredCounter = new int[]{0};

        currentPath.add(origin);
        currentPathSet.add(origin);

        dfsBacktrack(graph, origin, destination, aircraft, restrictedZones,
                currentPath, currentSegments, currentPathSet, results, maxDepth, maxPaths, nodesExploredCounter, startTime);

        return results;
    }

    private static void dfsBacktrack(Graph graph, Node current, Node destination,
                                    AircraftProfile aircraft, List<RestrictedZone> restrictedZones,
                                    List<Node> currentPath, List<FlightSegment> currentSegments,
                                    Set<Node> currentPathSet, List<RouteResult> results,
                                    int maxDepth, int maxPaths, int[] nodesExploredCounter, long startTime) {
        nodesExploredCounter[0]++;

        if (results.size() >= maxPaths) {
            return;
        }

        if (current.equals(destination)) {
            long executionTimeMicros = (System.nanoTime() - startTime) / 1000;
            double totalDist = 0.0, totalTime = 0.0, totalFuel = 0.0;
            for (FlightSegment seg : currentSegments) {
                totalDist += seg.getDistanceKm();
                totalTime += seg.getFlightTimeHours(aircraft);
                totalFuel += seg.getFuelConsumptionKg(aircraft);
            }
            results.add(new RouteResult("DFS Alternative #" + (results.size() + 1),
                    OptimizationObjective.DISTANCE, aircraft,
                    new ArrayList<>(currentPath), new ArrayList<>(currentSegments),
                    totalDist, totalTime, totalFuel, nodesExploredCounter[0],
                    executionTimeMicros, true, "Alternative path via DFS backtracking", null));
            return;
        }

        if (currentPath.size() > maxDepth) {
            return;
        }

        for (FlightSegment segment : graph.getOutgoingSegments(current)) {
            Node neighbor = segment.getTarget();

            if (!segment.isActive() || !neighbor.isActive()) continue;
            if (currentPathSet.contains(neighbor)) continue; // Avoid cycles

            if (restrictedZones != null && isSegmentRestricted(segment, aircraft, restrictedZones)) {
                continue;
            }

            // Choose
            currentPath.add(neighbor);
            currentPathSet.add(neighbor);
            currentSegments.add(segment);

            // Explore
            dfsBacktrack(graph, neighbor, destination, aircraft, restrictedZones,
                    currentPath, currentSegments, currentPathSet, results, maxDepth, maxPaths, nodesExploredCounter, startTime);

            // Backtrack
            currentPath.remove(currentPath.size() - 1);
            currentPathSet.remove(neighbor);
            currentSegments.remove(currentSegments.size() - 1);

            if (results.size() >= maxPaths) return;
        }
    }

    /**
     * Cycle detection in the flight network using 3-color DFS.
     * 0 = UNVISITED (White), 1 = VISITING (Gray, in current recursion stack), 2 = VISITED (Black).
     */
    public static boolean hasCycle(Graph graph) {
        Map<Node, Integer> state = new HashMap<>();
        for (Node node : graph.getAllNodes()) {
            state.put(node, 0);
        }

        for (Node node : graph.getAllNodes()) {
            if (state.get(node) == 0) {
                if (cycleDfs(graph, node, state)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean cycleDfs(Graph graph, Node current, Map<Node, Integer> state) {
        state.put(current, 1); // 1 = GRAY (currently in recursion path)

        for (FlightSegment seg : graph.getOutgoingSegments(current)) {
            Node next = seg.getTarget();
            Integer nextState = state.getOrDefault(next, 0);
            if (nextState == 1) {
                return true; // Found back-edge to ancestor in recursion tree -> CYCLE
            }
            if (nextState == 0) {
                if (cycleDfs(graph, next, state)) {
                    return true;
                }
            }
        }

        state.put(current, 2); // 2 = BLACK (fully explored)
        return false;
    }

    private static boolean isSegmentRestricted(FlightSegment segment, AircraftProfile aircraft, List<RestrictedZone> zones) {
        for (RestrictedZone zone : zones) {
            if (zone.intersectsSegment(segment, aircraft.getCruiseAltitudeMeters() * 3.28084)) {
                return true;
            }
        }
        return false;
    }
}
