package com.project.logistics.service;

import com.project.logistics.dto.RouteResult;
import com.project.logistics.dto.RouteStep;
import com.project.logistics.repository.PgRoutingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;

@Service
public class PgRoutingService {

    private static final Logger logger = Logger.getLogger(PgRoutingService.class.getName());
    private final PgRoutingRepository pgRoutingRepository;

    @Autowired
    public PgRoutingService(PgRoutingRepository pgRoutingRepository) {
        this.pgRoutingRepository = pgRoutingRepository;
    }

    /**
     * Finds the optimal route considering flooded areas.
     * Uses PostGIS and pgRouting under the hood.
     */
    public RouteResult findOptimalRoute(double startLat, double startLng, double destLat, double destLng) {
        try {
            logger.info(String.format("Searching for optimal route from (%f, %f) to (%f, %f) via PgRouting", 
                startLat, startLng, destLat, destLng));
                
            List<RouteStep> steps = pgRoutingRepository.findShortestPath(startLat, startLng, destLat, destLng);
            
            if (steps == null || steps.isEmpty()) {
                logger.warning("No route found between the given coordinates.");
                return new RouteResult(List.of(), 0.0);
            }

            double totalCost = steps.stream().mapToDouble(RouteStep::getCost).sum();
            
            logger.info("Found optimal route with " + steps.size() + " steps. Total Cost: " + totalCost);
            return new RouteResult(steps, totalCost);
            
        } catch (Exception ex) {
            // Because the database/extensions are not installed yet, this will fail gracefully.
            logger.severe("Failed to calculate route using PgRouting. Ensure PostgreSQL, PostGIS, and pgRouting are installed and configured.");
            logger.severe("Error details: " + ex.getMessage());
            
            // Fallback or empty result
            return new RouteResult(List.of(), 0.0);
        }
    }
}
