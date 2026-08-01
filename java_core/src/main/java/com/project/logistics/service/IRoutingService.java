package com.project.logistics.service;

import com.project.logistics.entities.Location;
import java.util.List;

public interface IRoutingService {
    /**
     * Finds the optimal route between start and destination using an external API.
     * @param start Starting location with latitude and longitude.
     * @param dest Destination location with latitude and longitude.
     * @return A list of locations representing the route path.
     */
    List<Location> getRoute(Location start, Location dest);
    
    /**
     * Gets the total distance of the optimal route in kilometers.
     */
    double getDistance(Location start, Location dest);
}
