package com.aerospace.flightpath.dsa.graph;

import com.aerospace.flightpath.model.FlightSegment;
import com.aerospace.flightpath.model.Node;
import com.aerospace.flightpath.model.WindVector;

import java.util.*;

/**
 * Graph representation using Adjacency Lists.
 * Optimal for sparse flight networks (space O(V + E), neighbor lookup O(deg(V))).
 */
public class AdjacencyListGraph implements Graph {
    private final Map<String, Node> nodesById = new LinkedHashMap<>();
    private final Map<Node, List<FlightSegment>> adjList = new HashMap<>();
    private final List<FlightSegment> allSegments = new ArrayList<>();

    @Override
    public void addNode(Node node) {
        if (node == null) return;
        nodesById.put(node.getId(), node);
        adjList.computeIfAbsent(node, k -> new ArrayList<>());
    }

    @Override
    public void addEdge(FlightSegment segment) {
        if (segment == null) return;
        addNode(segment.getSource());
        addNode(segment.getTarget());

        // Avoid exact duplicate edges
        List<FlightSegment> neighbors = adjList.get(segment.getSource());
        for (int i = 0; i < neighbors.size(); i++) {
            FlightSegment existing = neighbors.get(i);
            if (existing.getTarget().equals(segment.getTarget())) {
                neighbors.set(i, segment); // update
                return;
            }
        }
        neighbors.add(segment);
        allSegments.add(segment);
    }

    @Override
    public void addBidirectionalEdge(Node u, Node v, double windSpeedKnots, double windDirDegrees, String airwayCode) {
        WindVector wind = new WindVector(windDirDegrees, windSpeedKnots);
        // Note: For flight segments, wind heading applies equally to both directions,
        // but initial bearing differs, so computeEffectiveGroundSpeed handles headwind vs tailwind automatically!
        FlightSegment forward = new FlightSegment(u, v, wind, airwayCode, 1.0);
        FlightSegment backward = new FlightSegment(v, u, wind, airwayCode, 1.0);
        addEdge(forward);
        addEdge(backward);
    }

    @Override
    public Node getNode(String nodeId) {
        return nodesById.get(nodeId);
    }

    @Override
    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableCollection(nodesById.values());
    }

    @Override
    public Collection<FlightSegment> getAllSegments() {
        return Collections.unmodifiableCollection(allSegments);
    }

    @Override
    public List<FlightSegment> getOutgoingSegments(Node node) {
        List<FlightSegment> list = adjList.get(node);
        if (list == null) return Collections.emptyList();
        return Collections.unmodifiableList(list);
    }

    @Override
    public boolean hasEdge(Node from, Node to) {
        List<FlightSegment> list = adjList.get(from);
        if (list == null) return false;
        for (FlightSegment seg : list) {
            if (seg.getTarget().equals(to)) return true;
        }
        return false;
    }

    @Override
    public FlightSegment getEdge(Node from, Node to) {
        List<FlightSegment> list = adjList.get(from);
        if (list == null) return null;
        for (FlightSegment seg : list) {
            if (seg.getTarget().equals(to)) return seg;
        }
        return null;
    }

    @Override
    public int getNodeCount() {
        return nodesById.size();
    }

    @Override
    public int getEdgeCount() {
        return allSegments.size();
    }

    @Override
    public void setNodeActive(String nodeId, boolean active) {
        Node node = nodesById.get(nodeId);
        if (node != null) {
            node.setActive(active);
        }
    }

    @Override
    public void setAirwayActive(String airwayCode, boolean active) {
        for (FlightSegment seg : allSegments) {
            if (seg.getAirwayCode().equalsIgnoreCase(airwayCode)) {
                seg.setActive(active);
            }
        }
    }
}
