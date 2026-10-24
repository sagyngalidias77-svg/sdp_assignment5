package delivery;

import java.util.ArrayList;
import java.util.List;

public class StrategyComparison {
    public List<RouteDecision> compare(List<RouteStrategy> strategies,
            List<Route> routes, DeliveryRequest request) {
        List<RouteDecision> results = new ArrayList<>();
        for (RouteStrategy strategy : strategies) results.add(strategy.selectRoute(routes, request));
        return results;
    }
}
