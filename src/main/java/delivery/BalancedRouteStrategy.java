package delivery;

import java.util.List;
import java.util.Locale;

public class BalancedRouteStrategy extends ScoredRouteStrategy {
    private final double[] weights;
    public BalancedRouteStrategy(double time, double cost, double distance, double emission) {
        weights = new double[] {time, cost, distance, emission};
        double sum = 0;
        for (double weight : weights) {
            Validation.nonNegative(weight, "Weight");
            if (weight > 1) throw new IllegalArgumentException("Weight cannot exceed 1");
            sum += weight;
        }
        if (Math.abs(sum - 1.0) > 0.000000001) {
            throw new IllegalArgumentException("Weights must sum to 1.0");
        }
    }
    public String getStrategyName() { return "Balanced"; }
    private double[] values(Route route, DeliveryRequest request) {
        return new double[] {RouteRules.finalMinutes(route), RouteRules.totalCost(route, request),
                route.distanceKm(), route.carbonEmission()};
    }
    protected double score(Route route, DeliveryRequest request, List<Route> validRoutes) {
        double[] current = values(route, request);
        double total = 0;
        for (int criterion = 0; criterion < weights.length; criterion++) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (Route candidate : validRoutes) {
                double value = values(candidate, request)[criterion];
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            // Equal values contribute zero, including the single-route case.
            double normalized = max == min ? 0 : (current[criterion] - min) / (max - min);
            total += normalized * weights[criterion];
        }
        return total;
    }
    protected String explain(Route route, DeliveryRequest request, double score) {
        return String.format(Locale.ROOT,
                "Lowest weighted min-max score: %.4f; weights time=%.2f cost=%.2f distance=%.2f emission=%.2f; normalized over feasible routes",
                score, weights[0], weights[1], weights[2], weights[3]);
    }
}
