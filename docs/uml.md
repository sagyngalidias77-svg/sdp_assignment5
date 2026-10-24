# UML diagrams

## Class diagram

```mermaid
classDiagram
    class RouteStrategy {
        <<interface>>
        +selectRoute(List~Route~ routes, DeliveryRequest request) RouteDecision
        +getStrategyName() String
    }
    class RoutePlanner {
        -RouteStrategy strategy
        +setStrategy(RouteStrategy strategy) void
        +plan(List~Route~ routes, DeliveryRequest request) RouteDecision
    }
    class ScoredRouteStrategy {
        <<abstract>>
        +selectRoute(List~Route~ routes, DeliveryRequest request) RouteDecision
        #score(Route route, DeliveryRequest request, List~Route~ validRoutes) double
        #explain(Route route, DeliveryRequest request, double score) String
    }
    class FastestRouteStrategy
    class CheapestRouteStrategy
    class EcoFriendlyRouteStrategy
    class BalancedRouteStrategy {
        -double[] weights
    }
    class DistanceFirstStrategy
    class StrategySelector {
        -Map strategies
        +register(DeliveryPriority priority, RouteStrategy strategy) void
        +select(DeliveryRequest request) RouteStrategy
    }
    class StrategyComparison {
        +compare(List~RouteStrategy~ strategies, List~Route~ routes, DeliveryRequest request) List~RouteDecision~
    }
    class Route {
        <<record>>
        +String id
        +Location origin
        +Location destination
        +double distanceKm
        +int estimatedMinutes
        +double cost
        +double carbonEmission
        +int trafficPenaltyMinutes
        +double additionalFees
        +boolean available
    }
    class Location {
        <<record>>
        +String name
        +double latitude
        +double longitude
    }
    class DeliveryRequest {
        <<record>>
        +DeliveryPriority priority
        +double maxBudget
        +int maxDeliveryMinutes
        +boolean ecoPreferred
    }
    class DeliveryPriority {
        <<enumeration>>
        URGENT
        ECONOMY
        ECO
        STANDARD
    }
    class RouteDecision {
        <<record>>
        +Route route
        +String strategyName
        +double score
        +String reason
        +List~String~ rejectedRoutes
    }
    class RouteRules {
        +finalMinutes(Route route) long
        +totalCost(Route route, DeliveryRequest request) double
        +rejectionReasons(Route route, DeliveryRequest request) List~String~
    }
    class NoSuitableRouteException
    RoutePlanner o--> RouteStrategy : delegates
    RouteStrategy <|.. ScoredRouteStrategy
    ScoredRouteStrategy <|-- FastestRouteStrategy
    ScoredRouteStrategy <|-- CheapestRouteStrategy
    ScoredRouteStrategy <|-- EcoFriendlyRouteStrategy
    ScoredRouteStrategy <|-- BalancedRouteStrategy
    ScoredRouteStrategy <|-- DistanceFirstStrategy
    StrategySelector o--> RouteStrategy : registered strategies
    StrategySelector ..> DeliveryRequest : selects by request
    StrategyComparison ..> RouteStrategy : executes each
    ScoredRouteStrategy ..> RouteRules : filters before scoring
    ScoredRouteStrategy ..> RouteDecision : creates
    ScoredRouteStrategy ..> NoSuitableRouteException : throws if none valid
    RouteStrategy ..> Route : input candidates
    RouteStrategy ..> DeliveryRequest : constraints
    RouteStrategy ..> RouteDecision : result
    Route "*" --> "2" Location : origin and destination
    RouteDecision --> Route : winner
    DeliveryRequest --> DeliveryPriority
```

## Sequence diagram

Scenario: an urgent request with budget 6000 KZT and deadline 30 minutes. The planner already exists. `selectRoute` is inherited by FastestRouteStrategy from ScoredRouteStrategy.

```mermaid
sequenceDiagram
    actor Client as Main
    participant Selector as StrategySelector
    participant Planner as RoutePlanner
    participant Fastest as FastestRouteStrategy
    participant Rules as RouteRules
    participant Decision as RouteDecision
    Client->>Selector: select(urgentRequest)
    Selector-->>Client: registered FastestRouteStrategy
    Client->>Planner: setStrategy(fastest)
    Client->>Planner: plan(routes, urgentRequest)
    Planner->>Fastest: selectRoute(routes, urgentRequest)
    loop Each of eight candidate routes
        Fastest->>Rules: rejectionReasons(route, urgentRequest)
        Rules-->>Fastest: reasons or empty list
    end
    alt At least one feasible route
        loop Each feasible route
            Fastest->>Fastest: score(route, request, validRoutes)
            Fastest->>Rules: finalMinutes(route)
            Rules-->>Fastest: estimated time plus traffic
        end
        Fastest->>Fastest: explain(best, request, bestScore)
        Fastest->>Decision: new RouteDecision(R2, Fastest, 24, reason, rejected)
        Decision-->>Fastest: decision
        Fastest-->>Planner: decision
        Planner-->>Client: decision
    else No feasible route
        Fastest-->>Client: NoSuitableRouteException propagates through planner
    end
```
