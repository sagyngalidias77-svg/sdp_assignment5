package delivery;

import java.util.List;

public class DistanceFirstStrategy extends ScoredRouteStrategy {
    public String getStrategyName() { return "Distance first"; }
    protected double score(Route route, DeliveryRequest request, List<Route> validRoutes) {
        return route.distanceKm();
    }
    protected String explain(Route route, DeliveryRequest request, double score) {
        return "Shortest feasible distance: " + score + " km";
    }
}
