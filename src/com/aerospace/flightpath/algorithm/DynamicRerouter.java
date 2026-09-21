package com.aerospace.flightpath.algorithm;

import com.aerospace.flightpath.dsa.graph.Graph;
import com.aerospace.flightpath.model.*;

import java.util.*;

/**
 * Dynamic Rerouting Engine for In-Flight Contingencies.
 * Handles sudden waypoint closures (storm cells, navigation aid outages, congestion)
 * and newly declared Restricted Airspace / No-Fly Zones.
 */
public class DynamicRerouter {

    public static class RerouteAnalysis {
        private final RouteResult originalRoute;
        private final RouteResult reroutedRoute;
        private final Set<String> disabledNodeIds;
        private final List<RestrictedZone> activeZones;
        private final double distanceDeltaKm;
        private final double timeDeltaHours;
        private final double fuelDeltaKg;

        public RerouteAnalysis(RouteResult originalRoute, RouteResult reroutedRoute,
                               Set<String> disabledNodeIds, List<RestrictedZone> activeZones) {
            this.originalRoute = originalRoute;
            this.reroutedRoute = reroutedRoute;
            this.disabledNodeIds = disabledNodeIds;
            this.activeZones = activeZones;

            if (originalRoute != null && originalRoute.isSuccess() && reroutedRoute != null && reroutedRoute.isSuccess()) {
                this.distanceDeltaKm = reroutedRoute.getTotalDistanceKm() - originalRoute.getTotalDistanceKm();
                this.timeDeltaHours = reroutedRoute.getTotalTimeHours() - originalRoute.getTotalTimeHours();
                this.fuelDeltaKg = reroutedRoute.getTotalFuelKg() - originalRoute.getTotalFuelKg();
            } else {
                this.distanceDeltaKm = 0;
                this.timeDeltaHours = 0;
                this.fuelDeltaKg = 0;
            }
        }

        public RouteResult getOriginalRoute() {
            return originalRoute;
        }

        public RouteResult getReroutedRoute() {
            return reroutedRoute;
        }

        public Set<String> getDisabledNodeIds() {
            return disabledNodeIds;
        }

        public List<RestrictedZone> getActiveZones() {
            return activeZones;
        }

        public double getDistanceDeltaKm() {
            return distanceDeltaKm;
        }

        public double getTimeDeltaHours() {
            return timeDeltaHours;
        }

        public double getFuelDeltaKg() {
            return fuelDeltaKg;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== DYNAMIC REROUTE CONTINGENCY REPORT ===\n");
            sb.append(String.format("Disabled Waypoints: %s\n", disabledNodeIds));
            sb.append(String.format("Active No-Fly Zones: %d zones\n", activeZones.size()));
            if (reroutedRoute.isSuccess()) {
                sb.append(String.format("Original Distance: %.1f km  ->  Rerouted: %.1f km (Δ: %+.1f km)\n",
                        originalRoute.getTotalDistanceKm(), reroutedRoute.getTotalDistanceKm(), distanceDeltaKm));
                sb.append(String.format("Original Time:     %.2f hrs ->  Rerouted: %.2f hrs (Δ: %+.2f hrs)\n",
                        originalRoute.getTotalTimeHours(), reroutedRoute.getTotalTimeHours(), timeDeltaHours));
                sb.append(String.format("Original Fuel:     %.1f kg  ->  Rerouted: %.1f kg (Δ: %+.1f kg)\n",
                        originalRoute.getTotalFuelKg(), reroutedRoute.getTotalFuelKg(), fuelDeltaKg));
                sb.append("Rerouted Path: ");
                for (int i = 0; i < reroutedRoute.getPath().size(); i++) {
                    sb.append(reroutedRoute.getPath().get(i).getId());
                    if (i < reroutedRoute.getPath().size() - 1) sb.append(" -> ");
                }
                sb.append("\n");
            } else {
                sb.append("FAILURE: Unable to find alternate flight path around closures: " + reroutedRoute.getMessage() + "\n");
            }
            return sb.toString();
        }
    }

    /**
     * Computes the dynamic reroute when specified waypoints or restricted zones are triggered.
     */
    public static RerouteAnalysis planDetour(Graph graph, Node origin, Node destination,
                                            OptimizationObjective objective, AircraftProfile aircraft,
                                            Set<String> disabledNodeIds, List<RestrictedZone> restrictedZones) {
        // 1. Calculate baseline without current contingency
        RouteResult baseline = AStarRouter.findRoute(graph, origin, destination, objective, aircraft, Collections.emptyList());

        // 2. Temporarily disable designated waypoints
        Map<String, Boolean> priorNodeStates = new HashMap<>();
        if (disabledNodeIds != null) {
            for (String nodeId : disabledNodeIds) {
                Node node = graph.getNode(nodeId);
                if (node != null) {
                    priorNodeStates.put(nodeId, node.isActive());
                    node.setActive(false);
                }
            }
        }

        RouteResult rerouted;
        try {
            rerouted = AStarRouter.findRoute(graph, origin, destination, objective, aircraft, restrictedZones);
        } finally {
            // Restore node states
            for (Map.Entry<String, Boolean> entry : priorNodeStates.entrySet()) {
                Node node = graph.getNode(entry.getKey());
                if (node != null) {
                    node.setActive(entry.getValue());
                }
            }
        }

        return new RerouteAnalysis(baseline, rerouted,
                disabledNodeIds != null ? disabledNodeIds : Collections.emptySet(),
                restrictedZones != null ? restrictedZones : Collections.emptyList());
    }
}
