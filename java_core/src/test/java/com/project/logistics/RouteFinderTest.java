package com.project.logistics;

import com.project.logistics.entities.Location;
import com.project.logistics.service.IRoutingService;
import com.project.logistics.utils.RouteFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RouteFinderTest {

    @Mock
    private IRoutingService routingService;

    private RouteFinder routeFinder;

    @BeforeEach
    public void setUp() {
        routeFinder = new RouteFinder(routingService);
    }

    @Test
    public void testAStarRouteFinder_Success() {
        // Arrange
        Location start = new Location(21.0285, 105.8542, "Hanoi");
        Location dest = new Location(20.8449, 106.6881, "Hai Phong");
        
        List<Location> expectedRoute = Arrays.asList(
                start,
                new Location(20.9500, 106.0000, "Node 1"),
                dest
        );

        when(routingService.getRoute(start, dest)).thenReturn(expectedRoute);

        // Act
        List<Location> actualRoute = routeFinder.AStarRouteFinder(start, dest);

        // Assert
        assertNotNull(actualRoute);
        assertEquals(3, actualRoute.size());
        assertEquals("Hanoi", actualRoute.get(0).getAddress());
        assertEquals("Hai Phong", actualRoute.get(2).getAddress());
        verify(routingService, times(1)).getRoute(start, dest);
    }

    @Test
    public void testAStarRouteFinder_ApiFailure_Fallback() {
        // Arrange
        Location start = new Location(16.0471, 108.2068, "Da Nang");
        Location dest = new Location(16.4637, 107.5909, "Hue");
        
        // Giả lập API lỗi và trả về list 2 điểm (đường chim bay thẳng)
        List<Location> fallbackRoute = Arrays.asList(start, dest);
        when(routingService.getRoute(start, dest)).thenReturn(fallbackRoute);

        // Act
        List<Location> actualRoute = routeFinder.AStarRouteFinder(start, dest);

        // Assert
        assertNotNull(actualRoute);
        assertEquals(2, actualRoute.size());
        verify(routingService, times(1)).getRoute(start, dest);
    }
}
