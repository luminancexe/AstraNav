package com.aerospace.flightpath.model;

/**
 * Optimization objectives for flight path planning.
 */
public enum OptimizationObjective {
    DISTANCE("Shortest Geodesic Distance", "km"),
    TIME("Fastest Flight Time (Wind-Adjusted)", "hrs"),
    FUEL("Lowest Estimated Fuel Consumption", "kg");

    private final String displayName;
    private final String unit;

    OptimizationObjective(String displayName, String unit) {
        this.displayName = displayName;
        this.unit = unit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUnit() {
        return unit;
    }
}
