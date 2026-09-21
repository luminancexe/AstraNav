package com.aerospace.flightpath.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the computed flight path, performance metrics, and search algorithm diagnostics.
 */
public class RouteResult {
    private final String algorithmName;
    private final OptimizationObjective objective;
    private final AircraftProfile aircraft;
    private final List<Node> path;
    private final List<FlightSegment> segments;
    private final double totalDistanceKm;
    private final double totalTimeHours;
    private final double totalFuelKg;
    private final int nodesExplored;
    private final long executionTimeMicros;
    private final boolean success;
    private final String message;
    private final List<String> exploredNodeIds;

    public RouteResult(String algorithmName, OptimizationObjective objective, AircraftProfile aircraft,
                       List<Node> path, List<FlightSegment> segments,
                       double totalDistanceKm, double totalTimeHours, double totalFuelKg,
                       int nodesExplored, long executionTimeMicros,
                       boolean success, String message, List<String> exploredNodeIds) {
        this.algorithmName = algorithmName;
        this.objective = objective;
        this.aircraft = aircraft;
        this.path = path != null ? Collections.unmodifiableList(path) : Collections.emptyList();
        this.segments = segments != null ? Collections.unmodifiableList(segments) : Collections.emptyList();
        this.totalDistanceKm = totalDistanceKm;
        this.totalTimeHours = totalTimeHours;
        this.totalFuelKg = totalFuelKg;
        this.nodesExplored = nodesExplored;
        this.executionTimeMicros = executionTimeMicros;
        this.success = success;
        this.message = message;
        this.exploredNodeIds = exploredNodeIds != null ? Collections.unmodifiableList(exploredNodeIds) : Collections.emptyList();
    }

    public static RouteResult failure(String algorithmName, OptimizationObjective objective,
                                     AircraftProfile aircraft, String reason, int nodesExplored,
                                     long executionTimeMicros, List<String> exploredNodes) {
        return new RouteResult(algorithmName, objective, aircraft,
                Collections.emptyList(), Collections.emptyList(),
                0.0, 0.0, 0.0,
                nodesExplored, executionTimeMicros, false, reason, exploredNodes);
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public OptimizationObjective getObjective() {
        return objective;
    }

    public AircraftProfile getAircraft() {
        return aircraft;
    }

    public List<Node> getPath() {
        return path;
    }

    public List<FlightSegment> getSegments() {
        return segments;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public double getTotalDistanceNauticalMiles() {
        return totalDistanceKm * Coordinate.KM_TO_NAUTICAL_MILES;
    }

    public double getTotalTimeHours() {
        return totalTimeHours;
    }

    public String getFormattedFlightTime() {
        int hours = (int) totalTimeHours;
        int minutes = (int) Math.round((totalTimeHours - hours) * 60.0);
        return String.format("%02dh %02dm", hours, minutes);
    }

    public double getTotalFuelKg() {
        return totalFuelKg;
    }

    public int getNodesExplored() {
        return nodesExplored;
    }

    public long getExecutionTimeMicros() {
        return executionTimeMicros;
    }

    public double getExecutionTimeMillis() {
        return executionTimeMicros / 1000.0;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getExploredNodeIds() {
        return exploredNodeIds;
    }

    public int getHopCount() {
        return Math.max(0, path.size() - 1);
    }

    @Override
    public String toString() {
        if (!success) {
            return String.format("[%s] FAILED: %s (Explored: %d nodes, Time: %.2f ms)",
                    algorithmName, message, nodesExplored, getExecutionTimeMillis());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== [%s] %s ===\n", algorithmName, objective.getDisplayName()));
        sb.append(String.format("Route: %d waypoints (%d hops)\n", path.size(), getHopCount()));
        for (int i = 0; i < path.size(); i++) {
            sb.append(path.get(i).getId());
            if (i < path.size() - 1) sb.append(" -> ");
        }
        sb.append("\n");
        sb.append(String.format("Total Distance: %.1f km (%.1f NM)\n", totalDistanceKm, getTotalDistanceNauticalMiles()));
        sb.append(String.format("Flight Time:    %s (%.2f hours)\n", getFormattedFlightTime(), totalTimeHours));
        sb.append(String.format("Fuel Burn:      %.1f kg\n", totalFuelKg));
        sb.append(String.format("Search Stats:   %d nodes explored in %.3f ms\n", nodesExplored, getExecutionTimeMillis()));
        return sb.toString();
    }
}
