package com.aerospace.flightpath.dsa.graph;

import com.aerospace.flightpath.model.FlightSegment;
import com.aerospace.flightpath.model.Node;

import java.util.Collection;
import java.util.List;

/**
 * Common Graph contract for aerospace route planning.
 * Implementations provide either Adjacency List or Adjacency Matrix representations.
 */
public interface Graph {
    void addNode(Node node);

    void addEdge(FlightSegment segment);

    void addBidirectionalEdge(Node u, Node v, double windSpeedKnots, double windDirDegrees, String airwayCode);

    Node getNode(String nodeId);

    Collection<Node> getAllNodes();

    Collection<FlightSegment> getAllSegments();

    List<FlightSegment> getOutgoingSegments(Node node);

    boolean hasEdge(Node from, Node to);

    FlightSegment getEdge(Node from, Node to);

    int getNodeCount();

    int getEdgeCount();

    void setNodeActive(String nodeId, boolean active);

    void setAirwayActive(String airwayCode, boolean active);
}
