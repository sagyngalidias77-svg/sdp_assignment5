package delivery;

import java.util.Objects;

public record Route(String id, Location origin, Location destination,
        double distanceKm, int estimatedMinutes, double cost, double carbonEmission,
        int trafficPenaltyMinutes, double additionalFees, boolean available) {
    public Route {
        Validation.text(id, "Route id");
        Objects.requireNonNull(origin, "Origin is required");
        Objects.requireNonNull(destination, "Destination is required");
        Validation.nonNegative(distanceKm, "Distance");
        Validation.nonNegative(cost, "Base cost");
        Validation.nonNegative(carbonEmission, "Emissions");
        Validation.nonNegative(additionalFees, "Additional fees");
        if (estimatedMinutes < 0 || trafficPenaltyMinutes < 0) {
            throw new IllegalArgumentException("Time cannot be negative");
        }
    }
}
