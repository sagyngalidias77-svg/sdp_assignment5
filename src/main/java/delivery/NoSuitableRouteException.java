package delivery;

import java.util.List;

public class NoSuitableRouteException extends RuntimeException {
    private final List<String> reasons;
    public NoSuitableRouteException(List<String> reasons) {
        super("No suitable route: " + String.join("; ", reasons));
        this.reasons = List.copyOf(reasons);
    }
    public List<String> getReasons() { return reasons; }
}
