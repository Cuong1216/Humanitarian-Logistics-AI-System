package com.project.logistics.utils;

import com.project.logistics.entities.Location;
import com.project.logistics.service.IRoutingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteFinder {

    private final IRoutingService routingService;

    @Autowired
    public RouteFinder(IRoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * Finds the optimal path between start and destination using the configured routing service (OSRM).
     * This replaces the old hardcoded A* algorithm with a real-world map API.
     * 
     * @param start The starting location.
     * @param dest The destination location.
     * @return A list of locations representing the optimal path.
     */
    public List<Location> AStarRouteFinder(Location start, Location dest) {
        // We keep the old method name 'AStarRouteFinder' for compatibility with LogisticsController, 
        // but under the hood, we are now using the powerful routing API.
        return routingService.getRoute(start, dest);
    }
}
