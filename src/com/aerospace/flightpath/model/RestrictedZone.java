package com.aerospace.flightpath.model;

import java.util.Collections;
import java.util.List;

/**
 * Represents restricted airspace, temporary flight restrictions (TFR),
 * military operation areas (MOA), or severe convective weather storm cells.
 */
public class RestrictedZone {
    public enum ZoneType {
        MILITARY_PROHIBITED,
        SEVERE_STORM,
        VIP_TFR,
        VOLCANIC_ASH_CLOUD
    }

    private final String id;
    private final String name;
    private final ZoneType type;
    private final Coordinate center;
    private final double radiusKm;
    private final List<Coordinate> polygonVertices;
    private final double floorFt;
    private final double ceilingFt;
    private boolean active = true;

    /**
     * Circular restricted zone.
     */
    public RestrictedZone(String id, String name, ZoneType type, Coordinate center,
                          double radiusKm, double floorFt, double ceilingFt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.center = center;
        this.radiusKm = radiusKm;
        this.polygonVertices = Collections.emptyList();
        this.floorFt = floorFt;
        this.ceilingFt = ceilingFt;
    }

    /**
     * Polygonal restricted zone.
     */
    public RestrictedZone(String id, String name, ZoneType type, List<Coordinate> polygonVertices,
                          double floorFt, double ceilingFt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.polygonVertices = polygonVertices != null ? polygonVertices : Collections.emptyList();
        this.floorFt = floorFt;
        this.ceilingFt = ceilingFt;

        // Compute centroid
        if (!this.polygonVertices.isEmpty()) {
            double sumLat = 0.0;
            double sumLon = 0.0;
            for (Coordinate c : this.polygonVertices) {
                sumLat += c.getLatitude();
                sumLon += c.getLongitude();
            }
            this.center = new Coordinate(sumLat / this.polygonVertices.size(), sumLon / this.polygonVertices.size());
            // Approximate radius
            double maxDist = 0.0;
            for (Coordinate c : this.polygonVertices) {
                maxDist = Math.max(maxDist, this.center.haversineDistanceKm(c));
            }
            this.radiusKm = maxDist;
        } else {
            this.center = new Coordinate(0, 0);
            this.radiusKm = 0.0;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ZoneType getType() {
        return type;
    }

    public Coordinate getCenter() {
        return center;
    }

    public double getRadiusKm() {
        return radiusKm;
    }

    public List<Coordinate> getPolygonVertices() {
        return polygonVertices;
    }

    public double getFloorFt() {
        return floorFt;
    }

    public double getCeilingFt() {
        return ceilingFt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCircular() {
        return polygonVertices.isEmpty();
    }

    /**
     * Tests if a flight segment intersects this restricted zone in 3D (altitude + spatial).
     */
    public boolean intersectsSegment(FlightSegment segment, double flightAltitudeFt) {
        if (!active) return false;

        // Altitude vertical check
        if (flightAltitudeFt < floorFt || flightAltitudeFt > ceilingFt) {
            return false;
        }

        Coordinate p1 = segment.getSource().getCoordinate();
        Coordinate p2 = segment.getTarget().getCoordinate();

        if (isCircular()) {
            return segmentIntersectsCircle(p1, p2, center, radiusKm);
        } else {
            return segmentIntersectsPolygon(p1, p2, polygonVertices);
        }
    }

    /**
     * Line segment to circle intersection using vector projection on local tangent plane.
     */
    public static boolean segmentIntersectsCircle(Coordinate p1, Coordinate p2, Coordinate c, double radiusKm) {
        // Check if endpoints are inside circle
        if (p1.haversineDistanceKm(c) <= radiusKm || p2.haversineDistanceKm(c) <= radiusKm) {
            return true;
        }

        // Fast bounding box rejection
        double minLat = Math.min(p1.getLatitude(), p2.getLatitude()) - (radiusKm / 111.0);
        double maxLat = Math.max(p1.getLatitude(), p2.getLatitude()) + (radiusKm / 111.0);
        if (c.getLatitude() < minLat || c.getLatitude() > maxLat) {
            return false;
        }

        // Project to local Cartesian km coordinates centered at c
        double cosLat = Math.cos(Math.toRadians(c.getLatitude()));
        double x1 = (p1.getLongitude() - c.getLongitude()) * 111.320 * cosLat;
        double y1 = (p1.getLatitude() - c.getLatitude()) * 110.574;
        double x2 = (p2.getLongitude() - c.getLongitude()) * 111.320 * cosLat;
        double y2 = (p2.getLatitude() - c.getLatitude()) * 110.574;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double segLenSq = dx * dx + dy * dy;

        if (segLenSq < 1e-9) {
            return (x1 * x1 + y1 * y1) <= (radiusKm * radiusKm);
        }

        // Parametric projection t of origin (0,0) onto segment (x1, y1) -> (x2, y2)
        // vector from p1 to origin is (-x1, -y1)
        double t = ((-x1 * dx) + (-y1 * dy)) / segLenSq;
        t = Math.max(0.0, Math.min(1.0, t));

        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;

        double distSq = closestX * closestX + closestY * closestY;
        return distSq <= (radiusKm * radiusKm);
    }

    /**
     * Checks if line segment intersects a 2D spherical polygon.
     */
    public static boolean segmentIntersectsPolygon(Coordinate p1, Coordinate p2, List<Coordinate> poly) {
        if (poly.size() < 3) return false;

        // If either endpoint is inside polygon, it intersects
        if (pointInPolygon(p1, poly) || pointInPolygon(p2, poly)) {
            return true;
        }

        // Check intersection with each polygon edge
        for (int i = 0; i < poly.size(); i++) {
            Coordinate v1 = poly.get(i);
            Coordinate v2 = poly.get((i + 1) % poly.size());
            if (linesIntersect(p1.getLongitude(), p1.getLatitude(), p2.getLongitude(), p2.getLatitude(),
                    v1.getLongitude(), v1.getLatitude(), v2.getLongitude(), v2.getLatitude())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ray casting algorithm for point-in-polygon test.
     */
    public static boolean pointInPolygon(Coordinate pt, List<Coordinate> poly) {
        int n = poly.size();
        boolean inside = false;
        double x = pt.getLongitude();
        double y = pt.getLatitude();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = poly.get(i).getLongitude(), yi = poly.get(i).getLatitude();
            double xj = poly.get(j).getLongitude(), yj = poly.get(j).getLatitude();

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi + 1e-12) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Line segment intersection test between (x1, y1)-(x2, y2) and (x3, y3)-(x4, y4).
     */
    private static boolean linesIntersect(double x1, double y1, double x2, double y2,
                                         double x3, double y3, double x4, double y4) {
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(d) < 1e-12) return false;

        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / d;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / d;

        return (t >= 0.0 && t <= 1.0 && u >= 0.0 && u <= 1.0);
    }
}
