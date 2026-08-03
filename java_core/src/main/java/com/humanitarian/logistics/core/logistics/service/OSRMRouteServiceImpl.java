package com.humanitarian.logistics.core.logistics.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.humanitarian.logistics.core.logistics.dto.Location;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class OSRMRouteServiceImpl implements IRoutingService {

    private static final Logger logger = Logger.getLogger(OSRMRouteServiceImpl.class.getName());
    private static final String OSRM_BASE_URL = "http://router.project-osrm.org/route/v1/driving/";

    private final HttpClient httpClient;

    public OSRMRouteServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public List<Location> getRoute(Location start, Location dest) {
        List<Location> routePath = new ArrayList<>();
        try {
            // OSRM format: {longitude},{latitude};{longitude},{latitude}
            String coordinates = String.format(java.util.Locale.US, "%f,%f;%f,%f", 
                    start.getLongitude(), start.getLatitude(), 
                    dest.getLongitude(), dest.getLatitude());
            
            String url = OSRM_BASE_URL + coordinates + "?overview=full&geometries=geojson";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray routes = jsonResponse.getAsJsonArray("routes");
                
                if (routes != null && routes.size() > 0) {
                    JsonObject firstRoute = routes.get(0).getAsJsonObject();
                    JsonObject geometry = firstRoute.getAsJsonObject("geometry");
                    JsonArray coords = geometry.getAsJsonArray("coordinates");
                    
                    for (int i = 0; i < coords.size(); i++) {
                        JsonArray point = coords.get(i).getAsJsonArray();
                        double lon = point.get(0).getAsDouble();
                        double lat = point.get(1).getAsDouble();
                        routePath.add(new Location(lat, lon, "Path node " + i));
                    }
                }
            } else {
                logger.warning("OSRM API returned status code: " + response.statusCode());
            }

        } catch (Exception e) {
            logger.severe("Failed to fetch route from OSRM: " + e.getMessage());
        }
        
        if (routePath.isEmpty()) {
            // Fallback to straight line if API fails
            routePath.add(start);
            routePath.add(dest);
        }
        
        return routePath;
    }

    @Override
    public double getDistance(Location start, Location dest) {
        try {
            String coordinates = String.format(java.util.Locale.US, "%f,%f;%f,%f", 
                    start.getLongitude(), start.getLatitude(), 
                    dest.getLongitude(), dest.getLatitude());
            String url = OSRM_BASE_URL + coordinates + "?overview=false";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray routes = jsonResponse.getAsJsonArray("routes");
                
                if (routes != null && routes.size() > 0) {
                    JsonObject firstRoute = routes.get(0).getAsJsonObject();
                    double distanceMeters = firstRoute.get("distance").getAsDouble();
                    return distanceMeters / 1000.0; // Return in km
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to fetch distance from OSRM: " + e.getMessage());
        }
        
        return 0.0;
    }
}
