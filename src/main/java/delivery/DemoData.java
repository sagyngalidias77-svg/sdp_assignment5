package delivery;

import java.util.List;

public final class DemoData {
    private DemoData() { }
    public static List<Route> routes() {
        Location origin = new Location("Warehouse", 43.2389, 76.8897);
        Location destination = new Location("Customer", 43.2600, 76.9400);
        return List.of(
            new Route("R1", origin, destination, 12, 20, 2800, 4.5, 10, 300, true),
            new Route("R2", origin, destination, 15, 22, 3000, 5.0, 2, 500, true),
            new Route("R3", origin, destination, 8, 45, 700, 2.0, 5, 0, true),
            new Route("R4", origin, destination, 10, 40, 1600, 0.5, 3, 0, true),
            new Route("R5", origin, destination, 6, 35, 1800, 3.0, 8, 200, true),
            new Route("R6", origin, destination, 11, 28, 1700, 1.5, 4, 100, true),
            new Route("R7", origin, destination, 5, 10, 200, 0.1, 0, 0, false),
            new Route("R8", origin, destination, 9, 55, 500, 1.0, 10, 0, true));
    }
}
