package delivery;

import java.util.List;
import java.util.Locale;

public class Main {
    public static List<RouteStrategy> allStrategies() {
        return List.of(new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2));
    }
    private static void print(RouteDecision decision, DeliveryRequest request) {
        Route route = decision.route();
        System.out.printf(Locale.ROOT, "%s -> %s | time=%d min | cost=%.2f KZT | distance=%.1f km | CO2=%.1f kg | score=%.4f%n",
                decision.strategyName(), route.id(), RouteRules.finalMinutes(route),
                RouteRules.totalCost(route, request), route.distanceKm(), route.carbonEmission(), decision.score());
        System.out.println("  Reason: " + decision.reason());
        for (String rejected : decision.rejectedRoutes()) System.out.println("  Rejected " + rejected);
    }
    public static void main(String[] args) {
        List<Route> routes = DemoData.routes();
        DeliveryRequest standard = new DeliveryRequest(DeliveryPriority.STANDARD, 6000, 60, false);
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        System.out.println("=== SAME PLANNER: INITIAL STRATEGY AND THREE RUNTIME SWITCHES ===");
        print(planner.plan(routes, standard), standard);
        planner.setStrategy(new CheapestRouteStrategy());
        print(planner.plan(routes, standard), standard);
        planner.setStrategy(new EcoFriendlyRouteStrategy());
        print(planner.plan(routes, standard), standard);
        planner.setStrategy(new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2));
        print(planner.plan(routes, standard), standard);
        System.out.println("=== COMPARISON: SAME REQUEST, ALL STRATEGIES ===");
        for (RouteDecision result : new StrategyComparison().compare(allStrategies(), routes, standard)) {
            print(result, standard);
        }
        System.out.println("=== AUTOMATIC SELECTION AND DIFFERENT REQUESTS ===");
        StrategySelector selector = new StrategySelector();
        List<DeliveryRequest> requests = List.of(
                new DeliveryRequest(DeliveryPriority.URGENT, 6000, 30, false),
                new DeliveryRequest(DeliveryPriority.ECONOMY, 2000, 60, false),
                new DeliveryRequest(DeliveryPriority.ECO, 6000, 60, true),
                standard,
                new DeliveryRequest(DeliveryPriority.STANDARD, 6000, 60, true));
        for (DeliveryRequest request : requests) {
            System.out.println(request);
            planner.setStrategy(selector.select(request));
            print(planner.plan(routes, request), request);
        }
        System.out.println("=== SAME FASTEST STRATEGY WITH LOWER BUDGET ===");
        DeliveryRequest limited = new DeliveryRequest(DeliveryPriority.STANDARD, 3000, 60, false);
        planner.setStrategy(new FastestRouteStrategy());
        print(planner.plan(routes, limited), limited);
        System.out.println("=== THREE HANDLED FAILURE CASES ===");
        try { planner.plan(List.of(), standard); }
        catch (NoSuitableRouteException error) { System.out.println(error.getMessage()); }
        try { planner.plan(routes, new DeliveryRequest(DeliveryPriority.STANDARD, 100, 5, false)); }
        catch (NoSuitableRouteException error) { System.out.println(error.getMessage()); }
        try { new BalancedRouteStrategy(0.5, 0.5, 0.5, 0.5); }
        catch (IllegalArgumentException error) { System.out.println(error.getMessage()); }
    }
}
