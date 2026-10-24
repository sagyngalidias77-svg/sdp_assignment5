package delivery;

import java.util.List;

public class CheapestRouteStrategy extends ScoredRouteStrategy {
    public String getStrategyName() { return "Cheapest"; }
    protected double score(Route route, DeliveryRequest request, List<Route> validRoutes) {
        return RouteRules.totalCost(route, request);
    }
    protected String explain(Route route, DeliveryRequest request, double score) {
        double surcharge = request.priority() == DeliveryPriority.URGENT ? 500 : 0;
        return "Lowest total cost: base " + route.cost() + " + distance " + route.distanceKm()
                + " * 100 + priority " + surcharge + " + fees " + route.additionalFees()
                + " = " + score + " KZT";
    }
}
