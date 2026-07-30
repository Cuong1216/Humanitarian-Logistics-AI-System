package com.project.logistics.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a geographic road network for routing algorithms.
 */
public class RoadGraph {
    private final Map<Location, Map<Location, Double>> adjacencyList;

    public RoadGraph() {
        this.adjacencyList = new HashMap<>();
    }

    /**
     * Adds a new node to the graph if it doesn't already exist.
     */
    public void addNode(Location node) {
        adjacencyList.putIfAbsent(node, new HashMap<>());
    }

    /**
     * Adds an undirected edge between two nodes with a specified distance.
     */
    public void addEdge(Location source, Location dest, double distance) {
        addNode(source);
        addNode(dest);
        adjacencyList.get(source).put(dest, distance);
        adjacencyList.get(dest).put(source, distance); // Undirected graph
    }

    /**
     * Retrieves all neighbors for a given node and the distance to them.
     */
    public Map<Location, Double> getNeighbors(Location node) {
        return adjacencyList.getOrDefault(node, new HashMap<>());
    }
    
    /**
     * Retrieves all nodes in the graph.
     */
    public Set<Location> getNodes() {
        return adjacencyList.keySet();
    }
}
