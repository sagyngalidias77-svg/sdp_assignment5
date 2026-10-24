package delivery;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class StrategySelector {
    private final Map<DeliveryPriority, RouteStrategy> strategies = new EnumMap<>(DeliveryPriority.class);
    public StrategySelector() {
        register(DeliveryPriority.URGENT, new FastestRouteStrategy());
        register(DeliveryPriority.ECONOMY, new CheapestRouteStrategy());
        register(DeliveryPriority.ECO, new EcoFriendlyRouteStrategy());
        register(DeliveryPriority.STANDARD, new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2));
    }
    public void register(DeliveryPriority priority, RouteStrategy strategy) {
        strategies.put(Objects.requireNonNull(priority), Objects.requireNonNull(strategy));
    }
    public RouteStrategy select(DeliveryRequest request) {
        Objects.requireNonNull(request, "Request is required");
        // Eco preference overrides STANDARD only; urgency/economy stay explicit.
        DeliveryPriority key = request.priority();
        if (key == DeliveryPriority.STANDARD && request.ecoPreferred()) key = DeliveryPriority.ECO;
        return strategies.get(key);
    }
}
