package com.aerospace.flightpath.algorithm;

import com.aerospace.flightpath.dsa.graph.Graph;
import com.aerospace.flightpath.dsa.heap.IndexedMinHeap;
import com.aerospace.flightpath.model.*;

import java.util.*;

/**
 * A* (A-Star) Heuristic Search Flight Router.
 * Uses an admissible and consistent Great-Circle Haversine spatial heuristic.
 *
 * Evaluation Function: f(n) = g(n) + h(n)
 * - g(n): exact cost from origin to node n
 * - h(n): admissible heuristic from n to destination
 *
 * Guaranteed to find the optimal path while exploring significantly fewer nodes than Dijkstra.
 */
public class AStarRouter {

    public static RouteResult findRoute(Graph graph, Node origin, Node destination,
                                       OptimizationObjective objective, AircraftProfile aircraft,
                                       List<RestrictedZone> restrictedZones) {
        long startTime = System.nanoTime();

        if (graph == null || origin == null || destination == null) {
            return RouteResult.failure("A* Search", objective, aircraft, "Origin or destination is null", 0, 0, null);
        }

        if (!origin.isActive()) {
            return RouteResult.failure("A* Search", objective, aircraft, "Origin node is currently unavailable", 0, 0, null);
        }
        if (!destination.isActive()) {
            return RouteResult.failure("A* Search", objective, aircraft, "Destination node is currently unavailable", 0, 0, null);
        }

        if (origin.equals(destination)) {
            return new RouteResult("A* Search", objective, aircraft,
                    List.of(origin), Collections.emptyList(), 0, 0, 0, 1, 0, true, "Already at destination", List.of(origin.getId()));
        }

        Map<Node, Double> gScore = new HashMap<>(); // Cost from origin to node
        Map<Node, FlightSegment> edgeTo = new HashMap<>();
        Map<Node, Node> parentMap = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        List<String> exploredOrder = new ArrayList<>();

        // Priority queue indexed by f(n) = g(n) + h(n)
        IndexedMinHeap<Node> openSet = new IndexedMinHeap<>();

        gScore.put(origin, 0.0);
        double initialH = computeHeuristic(origin, destination, objective, aircraft);
        openSet.insertOrDecrease(origin, initialH);

        while (!openSet.isEmpty()) {
            Node current = openSet.extractMin();

            if (visited.contains(current)) continue;
            visited.add(current);
            exploredOrder.add(current.getId());

            if (current.equals(destination)) {
                break;
            }

            double currentG = gScore.get(current);

            for (FlightSegment segment : graph.getOutgoingSegments(current)) {
                Node neighbor = segment.getTarget();

                if (!segment.isActive() || !neighbor.isActive()) {
                    continue;
                }

                if (restrictedZones != null && isSegmentRestricted(segment, aircraft, restrictedZones)) {
                    continue;
                }

                double edgeWeight = segment.calculateCost(objective, aircraft);
                if (Double.isInfinite(edgeWeight)) continue;

                double tentativeG = currentG + edgeWeight;

                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    gScore.put(neighbor, tentativeG);
                    parentMap.put(neighbor, current);
                    edgeTo.put(neighbor, segment);

                    double h = computeHeuristic(neighbor, destination, objective, aircraft);
                    double f = tentativeG + h;

                    openSet.insertOrDecrease(neighbor, f);
                }
            }
        }

        long executionTimeMicros = (System.nanoTime() - startTime) / 1000;

        if (!parentMap.containsKey(destination)) {
            return RouteResult.failure("A* Search", objective, aircraft,
                    "No viable flight path found using A* heuristic search",
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

        double totalDist = 0.0;
        double totalTime = 0.0;
        double totalFuel = 0.0;

        for (FlightSegment seg : segments) {
            totalDist += seg.getDistanceKm();
            totalTime += seg.getFlightTimeHours(aircraft);
            totalFuel += seg.getFuelConsumptionKg(aircraft);
        }

        return new RouteResult("A* Search", objective, aircraft, path, segments,
                totalDist, totalTime, totalFuel, visited.size(), executionTimeMicros,
                true, "Optimal path computed with A* heuristic guidance", exploredOrder);
    }

    /**
     * Admissible and consistent spatial heuristic h(n) based on Great-Circle geodesic distance.
     */
    private static double computeHeuristic(Node from, Node to, OptimizationObjective objective, AircraftProfile aircraft) {
        double straightLineDistKm = from.distanceTo(to);

        switch (objective) {
            case DISTANCE:
                return straightLineDistKm;

            case TIME:
                // Upper bound on possible ground speed: cruise speed + maximum 150 knot jetstream tailwind
                double maxGroundSpeedKmh = aircraft.getCruiseAirspeedKmh() + (150 * 1.852);
                return straightLineDistKm / maxGroundSpeedKmh;

            case FUEL:
                // Minimum possible fuel burn: straight line with maximum favorable ground speed and 0 payload penalty
                double maxSpeed = aircraft.getCruiseAirspeedKmh() + (150 * 1.852);
                return aircraft.estimateFuelConsumptionKg(straightLineDistKm, maxSpeed, 0.0);

            default:
                return straightLineDistKm;
        }
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
