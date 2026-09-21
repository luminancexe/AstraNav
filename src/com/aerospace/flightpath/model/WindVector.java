package com.aerospace.flightpath.model;

/**
 * Atmospheric wind aloft vector.
 * directionDegrees: direction FROM which the wind is blowing (0-360, meteorological standard).
 * speedKnots: wind speed in knots (1 knot ≈ 1.852 km/h).
 */
public class WindVector {
    private final double directionDegrees; // Wind FROM direction (degrees)
    private final double speedKnots;        // Speed in knots

    public WindVector(double directionDegrees, double speedKnots) {
        this.directionDegrees = (directionDegrees % 360.0 + 360.0) % 360.0;
        this.speedKnots = Math.max(0.0, speedKnots);
    }

    public static WindVector calm() {
        return new WindVector(0.0, 0.0);
    }

    public double getDirectionDegrees() {
        return directionDegrees;
    }

    public double getSpeedKnots() {
        return speedKnots;
    }

    public double getSpeedKmh() {
        return speedKnots * 1.852;
    }

    /**
     * Computes the effective ground speed (km/h) for an aircraft with a given true airspeed (TAS km/h)
     * flying on a given true flight track (course in degrees).
     *
     * Headwind reduces ground speed; tailwind increases it.
     * Crosswind reduces forward component due to crab angle correction.
     */
    public double computeEffectiveGroundSpeedKmh(double trueAirspeedKmh, double flightCourseDegrees) {
        if (speedKnots <= 0.001) {
            return trueAirspeedKmh;
        }

        // Angle between flight course and the direction the wind is blowing TO
        // Wind FROM theta means blowing TO (theta + 180)
        double windBlowsToRad = Math.toRadians((directionDegrees + 180.0) % 360.0);
        double courseRad = Math.toRadians(flightCourseDegrees);

        double windSpeedKmh = getSpeedKmh();

        // Wind components relative to course
        double tailwindKmh = windSpeedKmh * Math.cos(windBlowsToRad - courseRad);
        double crosswindKmh = windSpeedKmh * Math.sin(windBlowsToRad - courseRad);

        // Wind triangle: ground speed with drift correction
        double forwardSpeedSq = (trueAirspeedKmh * trueAirspeedKmh) - (crosswindKmh * crosswindKmh);
        if (forwardSpeedSq <= 0.0) {
            // Wind exceeds aircraft capability, clamp to minimum safe crawl speed
            return 50.0;
        }

        double groundSpeed = Math.sqrt(forwardSpeedSq) + tailwindKmh;
        return Math.max(80.0, groundSpeed); // Aircraft must maintain minimum forward speed
    }

    @Override
    public String toString() {
        return String.format("%.0f° @ %.0f kts", directionDegrees, speedKnots);
    }
}
