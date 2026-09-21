package com.aerospace.flightpath.dsa.geometry;

import com.aerospace.flightpath.model.Coordinate;
import com.aerospace.flightpath.model.FlightSegment;

import java.util.List;

/**
 * Computational Geometry algorithms for Aerospace Airspace Management:
 * - Geodesic calculations (Haversine distance, initial bearing)
 * - Ray-Casting Point-in-Polygon
 * - Line Segment to Circle & Polygon Intersection
 */
public class AirspaceGeometry {

    /**
     * Ray-Casting algorithm for point-in-polygon containment test.
     * Time Complexity: O(V) where V is the number of vertices.
     */
    public static boolean isPointInPolygon(Coordinate point, List<Coordinate> polygon) {
        if (polygon == null || polygon.size() < 3) return false;
        boolean inside = false;
        double x = point.getLongitude();
        double y = point.getLatitude();
        int n = polygon.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i).getLongitude(), yi = polygon.get(i).getLatitude();
            double xj = polygon.get(j).getLongitude(), yj = polygon.get(j).getLatitude();

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi + 1e-12) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Determines whether a flight segment line intersects a circle with given center and radius (km).
     */
    public static boolean doesSegmentIntersectCircle(Coordinate p1, Coordinate p2, Coordinate center, double radiusKm) {
        if (p1.haversineDistanceKm(center) <= radiusKm || p2.haversineDistanceKm(center) <= radiusKm) {
            return true;
        }

        // Project to local Cartesian plane centered at 'center'
        double cosLat = Math.cos(Math.toRadians(center.getLatitude()));
        double x1 = (p1.getLongitude() - center.getLongitude()) * 111.320 * cosLat;
        double y1 = (p1.getLatitude() - center.getLatitude()) * 110.574;
        double x2 = (p2.getLongitude() - center.getLongitude()) * 111.320 * cosLat;
        double y2 = (p2.getLatitude() - center.getLatitude()) * 110.574;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double segLenSq = dx * dx + dy * dy;

        if (segLenSq < 1e-9) {
            return (x1 * x1 + y1 * y1) <= (radiusKm * radiusKm);
        }

        double t = ((-x1 * dx) + (-y1 * dy)) / segLenSq;
        t = Math.max(0.0, Math.min(1.0, t));

        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;

        return (closestX * closestX + closestY * closestY) <= (radiusKm * radiusKm);
    }
}
