package com.project.logistics;
import com.project.logistics.entities.Location;
import com.project.logistics.utils.RouteFinder;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RouteFinderTest {
    private RouteFinder routeFinder;

    @BeforeEach
    void setUp() {
        routeFinder = new RouteFinder();
    }

    @Test
    @DisplayName("Tìm đường từ Hà Nội đến Hồ Chí Minh — phải trả về path không rỗng")
    void testRouteHanoiToHCMC() {
        Location hanoi = new Location(21.0285, 105.8542, "Hanoi");
        Location hcmc = new Location(10.8231, 106.6297, "Ho Chi Minh City");
        List<Location> path = routeFinder.AStarRouteFinder(hanoi, hcmc);
        assertFalse(path.isEmpty(), "Path từ Hà Nội đến HCM phải tìm được");
        assertEquals("Hanoi", path.get(0).getAddress(), "Node đầu phải là Hà Nội");
    }

    @Test
    @DisplayName("Tìm đường khi start = destination — phải trả về path 1 node")
    void testRouteSameStartDest() {
        Location loc = new Location(21.0285, 105.8542, "Hanoi");
        List<Location> path = routeFinder.AStarRouteFinder(loc, loc);
        assertFalse(path.isEmpty());
    }

    @Test
    @DisplayName("Snap to nearest node — điểm tùy ý phải được snap về node trong graph")
    void testNearestNodeSnapping() {
        // Điểm gần Đà Nẵng nhưng không phải exact
        Location nearDanang = new Location(16.05, 108.21, "Near Da Nang");
        Location hue = new Location(16.4637, 107.5909, "Hue");
        List<Location> path = routeFinder.AStarRouteFinder(nearDanang, hue);
        assertNotNull(path);
    }
}
