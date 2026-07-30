package main.java.com.project.logistics.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import main.java.com.project.logistics.entities.Location;
import main.java.com.project.logistics.entities.RoadGraph;

public class RouteFinder {

    private RoadGraph demoGraph;

    public RouteFinder() {
        initDemoGraph();
    }

    /**
     * Initialize a demo graph with ~10 locations in Vietnam.
     */
    private void initDemoGraph() {
        demoGraph = new RoadGraph();

        Location hanoi = new Location(21.0285, 105.8542, "Hanoi");
        Location haiphong = new Location(20.8449, 106.6881, "Hai Phong");
        Location vinh = new Location(18.6796, 105.6813, "Vinh");
        Location hue = new Location(16.4637, 107.5909, "Hue");
        Location danang = new Location(16.0471, 108.2068, "Da Nang");
        Location quynhon = new Location(13.783, 109.2232, "Quy Nhon");
        Location nhatrang = new Location(12.2388, 109.1967, "Nha Trang");
        Location dalat = new Location(11.9404, 108.4583, "Da Lat");
        Location hcmc = new Location(10.8231, 106.6297, "Ho Chi Minh City");
        Location cantho = new Location(10.0452, 105.7469, "Can Tho");

        // Add edges (distances in km, computed via haversine)
        addEdge(hanoi, haiphong);
        addEdge(hanoi, vinh);
        addEdge(haiphong, vinh);
        addEdge(vinh, hue);
        addEdge(hue, danang);
        addEdge(danang, quynhon);
        addEdge(quynhon, nhatrang);
        addEdge(quynhon, dalat);
        addEdge(nhatrang, dalat);
        addEdge(nhatrang, hcmc);
        addEdge(dalat, hcmc);
        addEdge(hcmc, cantho);
    }

    private void addEdge(Location a, Location b) {
        double dist = calculateHaversine(a, b);
        demoGraph.addEdge(a, b, dist);
    }

    /**
     * Helper class for PriorityQueue to track the current node and its f(n) score.
     */
    private static class NodeRecord {
        Location node;
        double fScore;

        NodeRecord(Location node, double fScore) {
            this.node = node;
            this.fScore = fScore;
        }
    }

    /**
     * A* Route finder algorithm.
     * Finds the shortest path between start and destination using Haversine distance heuristic.
     * 
     * @param start The starting location.
     * @param dest The destination location.
     * @return A list of locations representing the optimal path.
     */
    public List<Location> AStarRouteFinder(Location start, Location dest) {
        // Snap start and dest to the nearest nodes in the graph if they aren't exactly in the graph
        Location startNode = getNearestNode(start);
        Location destNode = getNearestNode(dest);

        if (startNode == null || destNode == null) {
            return new ArrayList<>(); // Empty graph
        }

        // Open set: Priority queue sorted by f(n) = g(n) + h(n)
        PriorityQueue<NodeRecord> openSet = new PriorityQueue<>(Comparator.comparingDouble(nr -> nr.fScore));
        
        // Closed set: HashSet to keep track of visited nodes and avoid revisiting
        Set<Location> closedSet = new HashSet<>();
        
        // Maps to store the best path and cost
        Map<Location, Location> cameFrom = new HashMap<>();
        Map<Location, Double> gScore = new HashMap<>();
        
        for (Location node : demoGraph.getNodes()) {
            gScore.put(node, Double.POSITIVE_INFINITY);
        }
        gScore.put(startNode, 0.0);
        
        openSet.add(new NodeRecord(startNode, calculateHaversine(startNode, destNode)));

        while (!openSet.isEmpty()) {
            Location current = openSet.poll().node;

            // Stop condition: Reached the destination
            if (current.equals(destNode)) {
                return reconstructPath(cameFrom, current);
            }

            closedSet.add(current);

            for (Map.Entry<Location, Double> neighborEntry : demoGraph.getNeighbors(current).entrySet()) {
                Location neighbor = neighborEntry.getKey();
                double weight = neighborEntry.getValue();

                if (closedSet.contains(neighbor)) {
                    continue; // Skip already visited nodes
                }

                double tentativeGScore = gScore.get(current) + weight;

                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    // Found a better path to the neighbor
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + calculateHaversine(neighbor, destNode);
                    
                    openSet.add(new NodeRecord(neighbor, fScore));
                }
            }
        }

        // Return empty path if destination is unreachable
        return new ArrayList<>();
    }

    /**
     * Reconstructs the path from the cameFrom map.
     */
    private List<Location> reconstructPath(Map<Location, Location> cameFrom, Location current) {
        List<Location> path = new ArrayList<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Calculates the Haversine distance between two locations in kilometers.
     * This acts as our heuristic function h(n) and edge weight computation.
     */
    private double calculateHaversine(Location a, Location b) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(b.getLatitude() - a.getLatitude());
        double lonDistance = Math.toRadians(b.getLongitude() - a.getLongitude());
        double val = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(a.getLatitude())) * Math.cos(Math.toRadians(b.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(val), Math.sqrt(1 - val));
        return R * c;
    }

    /**
     * Snaps an arbitrary Location to the nearest node in the demo graph.
     */
    private Location getNearestNode(Location target) {
        Location nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (Location node : demoGraph.getNodes()) {
            double dist = calculateHaversine(target, node);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = node;
            }
        }
        return nearest;
    }
}
