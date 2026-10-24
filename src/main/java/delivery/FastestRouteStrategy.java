package delivery;

import java.util.List;

public class FastestRouteStrategy extends ScoredRouteStrategy {
    public String getStrategyName() { return "Fastest"; }
    protected double score(Route route, DeliveryRequest request, List<Route> validRoutes) {
        return RouteRules.finalMinutes(route);
    }
    protected String explain(Route route, DeliveryRequest request, double score) {
        return "Lowest final time: " + route.estimatedMinutes() + " + traffic "
                + route.trafficPenaltyMinutes() + " = " + RouteRules.finalMinutes(route) + " min";
    }
}
