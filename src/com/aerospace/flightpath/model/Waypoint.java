package com.aerospace.flightpath.model;

/**
 * Airway navigation waypoint (VOR, RNAV fix, Oceanic reporting point).
 */
public class Waypoint extends Node {
    private final String airwayIdentifier; // e.g. "NAT-A", "J121", "UL607"
    private final double minAltitudeFt;
    private final double maxAltitudeFt;

    public Waypoint(String id, String name, double latitude, double longitude,
                    String airwayIdentifier, double minAltitudeFt, double maxAltitudeFt,
                    NodeType type) {
        super(id, name, new Coordinate(latitude, longitude, (minAltitudeFt + maxAltitudeFt) / 2.0 * 0.3048), type);
        this.airwayIdentifier = airwayIdentifier;
        this.minAltitudeFt = minAltitudeFt;
        this.maxAltitudeFt = maxAltitudeFt;
    }

    public String getAirwayIdentifier() {
        return airwayIdentifier;
    }

    public double getMinAltitudeFt() {
        return minAltitudeFt;
    }

    public double getMaxAltitudeFt() {
        return maxAltitudeFt;
    }
}
