package com.aerospace.flightpath.algorithm;

import com.aerospace.flightpath.dsa.graph.Graph;
import com.aerospace.flightpath.dsa.heap.MinHeap;
import com.aerospace.flightpath.model.*;

import java.util.*;

/**
 * Yen's Algorithm for computing the K-Shortest Loopless Flight Paths.
 *
 * Provides pilots and flight dispatchers with optimal primary and backup alternate routes.
 * Utilizes DijkstraRouter as the sub-solver and a MinHeap priority queue of candidate paths.
 *
 * Time Complexity: O(K * V * (E + V log V))
 */
public class YenKShortestPaths {

    private static class PathCandidate implements Comparable<PathCandidate> {
        final List<Node> nodes;
        final List<FlightSegment> segments;
        final double cost;

        PathCandidate(List<Node> nodes, List<FlightSegment> segments, double cost) {
            this.nodes = nodes;
            this.segments = segments;
            this.cost = cost;
        }

        @Override
        public int compareTo(PathCandidate other) {
            return Double.compare(this.cost, other.cost);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PathCandidate that)) return false;
            return Objects.equals(nodes, that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(nodes);
        }
    }

    public static List<RouteResult> findKShortestPaths(Graph graph, Node origin, Node destination,
                                                      OptimizationObjective objective, AircraftProfile aircraft,
                                                      List<RestrictedZone> restrictedZones, int K) {
        long startTime = System.nanoTime();
        List<RouteResult> resultPaths = new ArrayList<>();
        if (K <= 0 || origin == null || destination == null) return resultPaths;

        // 1. Find 1st shortest path using Dijkstra
        RouteResult firstRoute = DijkstraRouter.findRoute(graph, origin, destination, objective, aircraft, restrictedZones);
        if (!firstRoute.isSuccess()) {
            return resultPaths;
        }

        List<PathCandidate> kShortest = new ArrayList<>();
        double firstCost = computePathCost(firstRoute.getSegments(), objective, aircraft);
        PathCandidate firstCandidate = new PathCandidate(firstRoute.getPath(), firstRoute.getSegments(), firstCost);
        kShortest.add(firstCandidate);

        resultPaths.add(new RouteResult("Optimal Route (Rank #1)", objective, aircraft,
                firstRoute.getPath(), firstRoute.getSegments(),
                firstRoute.getTotalDistanceKm(), firstRoute.getTotalTimeHours(), firstRoute.getTotalFuelKg(),
                firstRoute.getNodesExplored(), (System.nanoTime() - startTime) / 1000, true,
                "Primary optimal flight path", firstRoute.getExploredNodeIds()));

        // MinHeap to store candidate alternative paths (B)
        MinHeap<PathCandidate> candidateHeap = new MinHeap<>();
        Set<List<Node>> seenPaths = new HashSet<>();
        seenPaths.add(firstCandidate.nodes);

        List<FlightSegment> temporarilyDisabledSegments = new ArrayList<>();
        List<Node> temporarilyDisabledNodes = new ArrayList<>();

        try {
            for (int k = 1; k < K; k++) {
                PathCandidate prevPath = kShortest.get(k - 1);
                List<Node> prevNodes = prevPath.nodes;

                // The spur node ranges from the first node up to the next to last node in the previous k-shortest path
                for (int i = 0; i < prevNodes.size() - 1; i++) {
                    Node spurNode = prevNodes.get(i);
                    List<Node> rootPathNodes = new ArrayList<>(prevNodes.subList(0, i + 1));
                    List<FlightSegment> rootPathSegments = new ArrayList<>(prevPath.segments.subList(0, i));

                    // 1. Invalidate edges that are part of previously found paths that share the same root path
                    for (PathCandidate p : kShortest) {
                        if (p.nodes.size() > i && p.nodes.subList(0, i + 1).equals(rootPathNodes)) {
                            FlightSegment conflictingEdge = p.segments.get(i);
                            if (conflictingEdge.isActive()) {
                                conflictingEdge.setActive(false);
                                temporarilyDisabledSegments.add(conflictingEdge);
                            }
                        }
                    }

                    // 2. Temporarily disable root path nodes (except spur node) so spur path doesn't loop into root
                    for (int r = 0; r < rootPathNodes.size() - 1; r++) {
                        Node nodeToDisable = rootPathNodes.get(r);
                        if (nodeToDisable.isActive()) {
                            nodeToDisable.setActive(false);
                            temporarilyDisabledNodes.add(nodeToDisable);
                        }
                    }

                    // 3. Calculate the spur path from spurNode to destination
                    RouteResult spurRoute = DijkstraRouter.findRoute(graph, spurNode, destination, objective, aircraft, restrictedZones);

                    // Re-enable root path nodes and edges before evaluating candidate
                    restoreState(temporarilyDisabledSegments, temporarilyDisabledNodes);

                    if (spurRoute.isSuccess()) {
                        // Total path = root path + spur path
                        List<Node> totalNodes = new ArrayList<>(rootPathNodes);
                        List<Node> spurNodes = spurRoute.getPath();
                        for (int s = 1; s < spurNodes.size(); s++) {
                            totalNodes.add(spurNodes.get(s));
                        }

                        List<FlightSegment> totalSegments = new ArrayList<>(rootPathSegments);
                        totalSegments.addAll(spurRoute.getSegments());

                        if (!seenPaths.contains(totalNodes)) {
                            seenPaths.add(totalNodes);
                            double totalCost = computePathCost(totalSegments, objective, aircraft);
                            candidateHeap.insert(new PathCandidate(totalNodes, totalSegments, totalCost));
                        }
                    }
                }

                if (candidateHeap.isEmpty()) {
                    // No more alternate paths exist
                    break;
                }

                // Pick the candidate path with the lowest cost
                PathCandidate bestCandidate = candidateHeap.extractMin();
                kShortest.add(bestCandidate);

                double dist = 0.0, time = 0.0, fuel = 0.0;
                for (FlightSegment seg : bestCandidate.segments) {
                    dist += seg.getDistanceKm();
                    time += seg.getFlightTimeHours(aircraft);
                    fuel += seg.getFuelConsumptionKg(aircraft);
                }

                resultPaths.add(new RouteResult("Alternative Route (Rank #" + (k + 1) + ")",
                        objective, aircraft, bestCandidate.nodes, bestCandidate.segments,
                        dist, time, fuel, 0, (System.nanoTime() - startTime) / 1000,
                        true, "Alternate flight path generated by Yen's K-Shortest algorithm", null));
            }
        } finally {
            // Guarantee all temporary modifications are reverted
            restoreState(temporarilyDisabledSegments, temporarilyDisabledNodes);
        }

        return resultPaths;
    }

    private static double computePathCost(List<FlightSegment> segments, OptimizationObjective objective, AircraftProfile aircraft) {
        double cost = 0.0;
        for (FlightSegment seg : segments) {
            cost += seg.calculateCost(objective, aircraft);
        }
        return cost;
    }

    private static void restoreState(List<FlightSegment> segments, List<Node> nodes) {
        for (FlightSegment seg : segments) {
            seg.setActive(true);
        }
        segments.clear();

        for (Node node : nodes) {
            node.setActive(true);
        }
        nodes.clear();
    }
}
