package com.aerospace.flightpath.algorithm;

import com.aerospace.flightpath.dsa.graph.Graph;
import com.aerospace.flightpath.dsa.heap.IndexedMinHeap;
import com.aerospace.flightpath.model.*;

import java.util.*;

/**
 * Classical Dijkstra's Shortest Path Algorithm implemented using a custom IndexedMinHeap.
 * Supports multi-objective optimization (Distance, Time, Fuel) and dynamic restricted airspace avoidance.
 *
 * Time Complexity: O((V + E) log V)
 * Space Complexity: O(V)
 */
public class DijkstraRouter {

    public static RouteResult findRoute(Graph graph, Node origin, Node destination,
                                       OptimizationObjective objective, AircraftProfile aircraft,
                                       List<RestrictedZone> restrictedZones) {
        long startTime = System.nanoTime();

        if (graph == null || origin == null || destination == null) {
            return RouteResult.failure("Dijkstra", objective, aircraft, "Origin or destination is null", 0, 0, null);
        }

        if (!origin.isActive()) {
            return RouteResult.failure("Dijkstra", objective, aircraft, "Origin node is currently unavailable", 0, 0, null);
        }
        if (!destination.isActive()) {
            return RouteResult.failure("Dijkstra", objective, aircraft, "Destination node is currently unavailable", 0, 0, null);
        }

        if (origin.equals(destination)) {
            return new RouteResult("Dijkstra", objective, aircraft,
                    List.of(origin), Collections.emptyList(), 0, 0, 0, 1, 0, true, "Already at destination", List.of(origin.getId()));
        }

        Map<Node, Double> minCost = new HashMap<>();
        Map<Node, FlightSegment> edgeTo = new HashMap<>();
        Map<Node, Node> parentMap = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        List<String> exploredOrder = new ArrayList<>();

        IndexedMinHeap<Node> pq = new IndexedMinHeap<>();

        minCost.put(origin, 0.0);
        pq.insertOrDecrease(origin, 0.0);

        while (!pq.isEmpty()) {
            Node current = pq.extractMin();

            if (visited.contains(current)) continue;
            visited.add(current);
            exploredOrder.add(current.getId());

            // Target reached
            if (current.equals(destination)) {
                break;
            }

            double currentCost = minCost.get(current);

            for (FlightSegment segment : graph.getOutgoingSegments(current)) {
                Node neighbor = segment.getTarget();

                if (!segment.isActive() || !neighbor.isActive()) {
                    continue;
                }

                // Check restricted airspace collision
                if (restrictedZones != null && isSegmentRestricted(segment, aircraft, restrictedZones)) {
                    continue;
                }

                double edgeWeight = segment.calculateCost(objective, aircraft);
                if (Double.isInfinite(edgeWeight)) continue;

                double newCost = currentCost + edgeWeight;

                if (newCost < minCost.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    minCost.put(neighbor, newCost);
                    parentMap.put(neighbor, current);
                    edgeTo.put(neighbor, segment);
                    pq.insertOrDecrease(neighbor, newCost);
                }
            }
        }

        long executionTimeMicros = (System.nanoTime() - startTime) / 1000;

        if (!parentMap.containsKey(destination)) {
            return RouteResult.failure("Dijkstra", objective, aircraft,
                    "No viable flight path exists (airways blocked or no connected path)",
                    visited.size(), executionTimeMicros, exploredOrder);
        }

        // Reconstruct path
        List<Node> path = new ArrayList<>();
        List<FlightSegment> segments = new ArrayList<>();
        Node curr = destination;

        while (curr != null) {
            path.add(curr);
            FlightSegment seg = edgeTo.get(curr);
            if (seg != null) {
                segments.add(seg);
            }
            curr = parentMap.get(curr);
        }

        Collections.reverse(path);
        Collections.reverse(segments);

        // Compute total stats
        double totalDist = 0.0;
        double totalTime = 0.0;
        double totalFuel = 0.0;

        for (FlightSegment seg : segments) {
            totalDist += seg.getDistanceKm();
            totalTime += seg.getFlightTimeHours(aircraft);
            totalFuel += seg.getFuelConsumptionKg(aircraft);
        }

        return new RouteResult("Dijkstra", objective, aircraft, path, segments,
                totalDist, totalTime, totalFuel, visited.size(), executionTimeMicros,
                true, "Optimal path computed successfully", exploredOrder);
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
