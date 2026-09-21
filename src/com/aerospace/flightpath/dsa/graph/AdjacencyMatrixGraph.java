package com.aerospace.flightpath.dsa.graph;

import com.aerospace.flightpath.model.FlightSegment;
import com.aerospace.flightpath.model.Node;
import com.aerospace.flightpath.model.WindVector;

import java.util.*;

/**
 * Graph representation using an Adjacency Matrix.
 * Space: O(V^2), Edge check: O(1).
 * Excellent for dense airway meshes and direct connectivity checks.
 */
public class AdjacencyMatrixGraph implements Graph {
    private static final int INITIAL_CAPACITY = 64;

    private FlightSegment[][] matrix;
    private final List<Node> nodes = new ArrayList<>();
    private final Map<String, Integer> idToIndex = new HashMap<>();
    private final List<FlightSegment> allSegments = new ArrayList<>();

    public AdjacencyMatrixGraph() {
        this(INITIAL_CAPACITY);
    }

    public AdjacencyMatrixGraph(int capacity) {
        matrix = new FlightSegment[capacity][capacity];
    }

    /**
     * Constructs an Adjacency Matrix Graph directly from an existing Adjacency List Graph.
     */
    public static AdjacencyMatrixGraph fromGraph(Graph sourceGraph) {
        AdjacencyMatrixGraph matrixGraph = new AdjacencyMatrixGraph(Math.max(INITIAL_CAPACITY, sourceGraph.getNodeCount()));
        for (Node node : sourceGraph.getAllNodes()) {
            matrixGraph.addNode(node);
        }
        for (FlightSegment seg : sourceGraph.getAllSegments()) {
            matrixGraph.addEdge(seg);
        }
        return matrixGraph;
    }

    @Override
    public synchronized void addNode(Node node) {
        if (node == null || idToIndex.containsKey(node.getId())) return;
        int index = nodes.size();
        ensureCapacity(index + 1);
        nodes.add(node);
        idToIndex.put(node.getId(), index);
    }

    @Override
    public synchronized void addEdge(FlightSegment segment) {
        if (segment == null) return;
        addNode(segment.getSource());
        addNode(segment.getTarget());

        int u = idToIndex.get(segment.getSource().getId());
        int v = idToIndex.get(segment.getTarget().getId());

        if (matrix[u][v] == null) {
            allSegments.add(segment);
        } else {
            // Replace existing in allSegments list
            allSegments.remove(matrix[u][v]);
            allSegments.add(segment);
        }
        matrix[u][v] = segment;
    }

    @Override
    public void addBidirectionalEdge(Node u, Node v, double windSpeedKnots, double windDirDegrees, String airwayCode) {
        WindVector wind = new WindVector(windDirDegrees, windSpeedKnots);
        addEdge(new FlightSegment(u, v, wind, airwayCode, 1.0));
        addEdge(new FlightSegment(v, u, wind, airwayCode, 1.0));
    }

    @Override
    public Node getNode(String nodeId) {
        Integer idx = idToIndex.get(nodeId);
        return idx != null ? nodes.get(idx) : null;
    }

    @Override
    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public Collection<FlightSegment> getAllSegments() {
        return Collections.unmodifiableList(allSegments);
    }

    @Override
    public List<FlightSegment> getOutgoingSegments(Node node) {
        Integer u = idToIndex.get(node.getId());
        if (u == null) return Collections.emptyList();

        List<FlightSegment> result = new ArrayList<>();
        for (int v = 0; v < nodes.size(); v++) {
            FlightSegment seg = matrix[u][v];
            if (seg != null) {
                result.add(seg);
            }
        }
        return result;
    }

    @Override
    public boolean hasEdge(Node from, Node to) {
        Integer u = idToIndex.get(from.getId());
        Integer v = idToIndex.get(to.getId());
        if (u == null || v == null) return false;
        return matrix[u][v] != null;
    }

    @Override
    public FlightSegment getEdge(Node from, Node to) {
        Integer u = idToIndex.get(from.getId());
        Integer v = idToIndex.get(to.getId());
        if (u == null || v == null) return null;
        return matrix[u][v];
    }

    @Override
    public int getNodeCount() {
        return nodes.size();
    }

    @Override
    public int getEdgeCount() {
        return allSegments.size();
    }

    @Override
    public void setNodeActive(String nodeId, boolean active) {
        Node node = getNode(nodeId);
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

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > matrix.length) {
            int newCap = Math.max(minCapacity, matrix.length * 2);
            FlightSegment[][] newMatrix = new FlightSegment[newCap][newCap];
            for (int i = 0; i < matrix.length; i++) {
                System.arraycopy(matrix[i], 0, newMatrix[i], 0, matrix.length);
            }
            matrix = newMatrix;
        }
    }
}
