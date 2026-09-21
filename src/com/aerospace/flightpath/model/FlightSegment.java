package com.aerospace.flightpath.model;

import java.util.Objects;

/**
 * Directed flight segment (edge) connecting two aeronautical nodes.
 * Encapsulates geodesic distance, bearing, wind conditions, and multi-objective cost calculation.
 */
public class FlightSegment {
    private final Node source;
    private final Node target;
    private final double distanceKm;
    private final double initialBearingDegrees;
    private final WindVector windVector;
    private final String airwayCode;
    private final double turbulenceMultiplier; // 1.0 = smooth, 1.5 = moderate turbulence, 2.0 = severe
    private boolean active = true;

    public FlightSegment(Node source, Node target, WindVector windVector, String airwayCode, double turbulenceMultiplier) {
        this.source = source;
        this.target = target;
        this.distanceKm = source.distanceTo(target);
        this.initialBearingDegrees = source.getCoordinate().initialBearingDegrees(target.getCoordinate());
        this.windVector = windVector != null ? windVector : WindVector.calm();
        this.airwayCode = airwayCode != null ? airwayCode : "DIRECT";
        this.turbulenceMultiplier = Math.max(1.0, turbulenceMultiplier);
    }

    public FlightSegment(Node source, Node target, WindVector windVector) {
        this(source, target, windVector, "DIRECT", 1.0);
    }

    public FlightSegment(Node source, Node target) {
        this(source, target, WindVector.calm(), "DIRECT", 1.0);
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getInitialBearingDegrees() {
        return initialBearingDegrees;
    }

    public WindVector getWindVector() {
        return windVector;
    }

    public String getAirwayCode() {
        return airwayCode;
    }

    public double getTurbulenceMultiplier() {
        return turbulenceMultiplier;
    }

    public boolean isActive() {
        return active && source.isActive() && target.isActive();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Calculates the cost of traversing this flight segment according to the given optimization objective.
     */
    public double calculateCost(OptimizationObjective objective, AircraftProfile aircraft) {
        if (!isActive()) {
            return Double.POSITIVE_INFINITY;
        }

        switch (objective) {
            case DISTANCE:
                return distanceKm;

            case TIME:
                double groundSpeedKmh = windVector.computeEffectiveGroundSpeedKmh(
                        aircraft.getCruiseAirspeedKmh(), initialBearingDegrees);
                return distanceKm / groundSpeedKmh; // Flight duration in hours

            case FUEL:
                double speed = windVector.computeEffectiveGroundSpeedKmh(
                        aircraft.getCruiseAirspeedKmh(), initialBearingDegrees);
                double fuelKg = aircraft.estimateFuelConsumptionKg(distanceKm, speed, 0.7);
                return fuelKg * turbulenceMultiplier;

            default:
                return distanceKm;
        }
    }

    public double getEffectiveGroundSpeedKmh(AircraftProfile aircraft) {
        return windVector.computeEffectiveGroundSpeedKmh(aircraft.getCruiseAirspeedKmh(), initialBearingDegrees);
    }

    public double getFlightTimeHours(AircraftProfile aircraft) {
        double speed = getEffectiveGroundSpeedKmh(aircraft);
        return distanceKm / speed;
    }

    public double getFuelConsumptionKg(AircraftProfile aircraft) {
        double speed = getEffectiveGroundSpeedKmh(aircraft);
        return aircraft.estimateFuelConsumptionKg(distanceKm, speed, 0.7) * turbulenceMultiplier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightSegment that)) return false;
        return Objects.equals(source, that.source) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public String toString() {
        return String.format("%s -> %s (%.1f km, %s, %s)",
                source.getId(), target.getId(), distanceKm, airwayCode, windVector);
    }
}
