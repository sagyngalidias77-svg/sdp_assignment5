package delivery;

import java.util.ArrayList;
import java.util.List;

public final class RouteRules {
    private RouteRules() { }
    public static long finalMinutes(Route route) {
        return (long) route.estimatedMinutes() + route.trafficPenaltyMinutes();
    }
    public static double totalCost(Route route, DeliveryRequest request) {
        double prioritySurcharge = request.priority() == DeliveryPriority.URGENT ? 500 : 0;
        return route.cost() + route.distanceKm() * 100 + prioritySurcharge + route.additionalFees();
    }
    public static List<String> rejectionReasons(Route route, DeliveryRequest request) {
        List<String> reasons = new ArrayList<>();
        if (!route.available()) reasons.add("Route unavailable");
        if (totalCost(route, request) > request.maxBudget()) reasons.add("Budget exceeded");
        if (finalMinutes(route) > request.maxDeliveryMinutes()) reasons.add("Delivery deadline exceeded");
        return reasons;
    }
}
