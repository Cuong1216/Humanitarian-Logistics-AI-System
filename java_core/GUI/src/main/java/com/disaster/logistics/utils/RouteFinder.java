package com.disaster.logistics.utils;

import com.disaster.logistics.entities.Location;
import java.util.ArrayList;
import java.util.List;

public class RouteFinder {

    /**
     * A* Route finder (simplified mock for demonstration).
     * Returns a list of waypoints from start to destination.
     */
    public List<Location> AStarRouteFinder(Location start, Location dest) {
        List<Location> route = new ArrayList<>();
        route.add(start);

        // Mock intermediate waypoints
        double latStep = (dest.getLatitude() - start.getLatitude()) / 3;
        double lngStep = (dest.getLongitude() - start.getLongitude()) / 3;
        for (int i = 1; i <= 2; i++) {
            route.add(new Location(
                start.getLatitude() + latStep * i,
                start.getLongitude() + lngStep * i,
                "Waypoint " + i
            ));
        }

        route.add(dest);
        return route;
    }
}
