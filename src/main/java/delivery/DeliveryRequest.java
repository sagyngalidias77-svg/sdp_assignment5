package delivery;

import java.util.Objects;

public record DeliveryRequest(DeliveryPriority priority, double maxBudget,
        int maxDeliveryMinutes, boolean ecoPreferred) {
    public DeliveryRequest {
        Objects.requireNonNull(priority, "Priority is required");
        Validation.nonNegative(maxBudget, "Budget");
        if (maxDeliveryMinutes < 0) throw new IllegalArgumentException("Deadline cannot be negative");
    }
}
