package com.aerospace.flightpath.model;

import java.util.Objects;

/**
 * Base graph node representing an aeronautical navigation fix, airport, or drone vertiport.
 */
public class Node {
    public enum NodeType {
        MAJOR_HUB,
        REGIONAL_AIRPORT,
        AIRWAY_WAYPOINT,
        OCEANIC_FIX,
        DRONE_VERTIPORT
    }

    private final String id;
    private final String name;
    private final Coordinate coordinate;
    private final NodeType type;
    private boolean active = true;

    public Node(String id, String name, Coordinate coordinate, NodeType type) {
        this.id = id;
        this.name = name;
        this.coordinate = coordinate;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public NodeType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isAirport() {
        return type == NodeType.MAJOR_HUB || type == NodeType.REGIONAL_AIRPORT;
    }

    public double distanceTo(Node other) {
        return this.coordinate.haversineDistanceKm(other.coordinate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node otherNode)) return false;
        return Objects.equals(id, otherNode.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("%s [%s] (%s)", name, id, type);
    }
}
