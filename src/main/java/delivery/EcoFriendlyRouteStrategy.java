package delivery;

import java.util.List;

public class EcoFriendlyRouteStrategy extends ScoredRouteStrategy {
    public String getStrategyName() { return "Eco"; }
    protected double score(Route route, DeliveryRequest request, List<Route> validRoutes) {
        return route.carbonEmission() * 0.8 + route.distanceKm() * 0.2;
    }
    protected String explain(Route route, DeliveryRequest request, double score) {
        return "Lowest eco score: " + route.carbonEmission() + " * 0.8 + "
                + route.distanceKm() + " * 0.2 = " + score;
    }
}
