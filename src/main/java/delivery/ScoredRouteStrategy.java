package delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Shares filtering and minimum selection; subclasses define the objective.
public abstract class ScoredRouteStrategy implements RouteStrategy {
    protected abstract double score(Route route, DeliveryRequest request, List<Route> validRoutes);
    protected abstract String explain(Route route, DeliveryRequest request, double score);

    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        Objects.requireNonNull(routes, "Routes are required");
        Objects.requireNonNull(request, "Request is required");
        List<Route> valid = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (Route route : routes) {
            Objects.requireNonNull(route, "Route cannot be null");
            List<String> reasons = RouteRules.rejectionReasons(route, request);
            if (reasons.isEmpty()) valid.add(route);
            else rejected.add(route.id() + ": " + String.join(", ", reasons));
        }
        if (valid.isEmpty()) {
            if (routes.isEmpty()) rejected.add("Route list is empty");
            throw new NoSuitableRouteException(rejected);
        }
        Route best = valid.get(0);
        double bestScore = score(best, request, valid);
        for (int i = 1; i < valid.size(); i++) {
            Route candidate = valid.get(i);
            double candidateScore = score(candidate, request, valid);
            if (candidateScore < bestScore) {
                best = candidate;
                bestScore = candidateScore;
            }
        }
        return new RouteDecision(best, getStrategyName(), bestScore,
                explain(best, request, bestScore), rejected);
    }
}
