package com.aerospace.flightpath.model;

/**
 * 3D/2D Geodetic coordinate representing latitude, longitude, and altitude.
 * Implements standard aeronautical geodesy formulas (Great-Circle Haversine, initial bearing).
 */
public class Coordinate {
    public static final double EARTH_RADIUS_KM = 6371.0;
    public static final double KM_TO_NAUTICAL_MILES = 0.539957;

    private final double latitude;   // Degrees [-90, 90]
    private final double longitude;  // Degrees [-180, 180]
    private final double altitudeMeters;

    public Coordinate(double latitude, double longitude) {
        this(latitude, longitude, 10000.0); // Default cruise altitude ~33,000 ft
    }

    public Coordinate(double latitude, double longitude, double altitudeMeters) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitudeMeters() {
        return altitudeMeters;
    }

    public double getAltitudeFeet() {
        return altitudeMeters * 3.28084;
    }

    /**
     * Calculates the great-circle distance to another coordinate using the Haversine formula.
     * Guaranteed to be non-negative and symmetric: d(A, B) == d(B, A).
     *
     * @param other Target coordinate
     * @return Distance in kilometers
     */
    public double haversineDistanceKm(Coordinate other) {
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLatRad = Math.toRadians(other.latitude - this.latitude);
        double deltaLonRad = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(deltaLatRad / 2.0) * Math.sin(deltaLatRad / 2.0)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLonRad / 2.0) * Math.sin(deltaLonRad / 2.0);

        // Numerical safety clamp
        a = Math.min(1.0, Math.max(0.0, a));
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculates initial true bearing (compass heading) from this coordinate to target in degrees [0, 360).
     */
    public double initialBearingDegrees(Coordinate target) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(target.latitude);
        double dLon = Math.toRadians(target.longitude - this.longitude);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearingRad = Math.atan2(y, x);
        double bearingDeg = Math.toDegrees(bearingRad);
        return (bearingDeg + 360.0) % 360.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coordinate that)) return false;
        return Double.compare(that.latitude, latitude) == 0 &&
               Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(latitude);
        result = 31 * result + Double.hashCode(longitude);
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%.4f°, %.4f°, %.0fm)", latitude, longitude, altitudeMeters);
    }
}
