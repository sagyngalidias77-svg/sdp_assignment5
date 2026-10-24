# Smart Delivery and Route Optimization Engine

Assignment 5 implements the Strategy Pattern in Java 17. It selects one delivery route from eight candidate routes. Each algorithm first rejects unavailable, over-budget and late routes, then chooses the best feasible route for its objective. The console application demonstrates interchangeable algorithms, automatic selection, explanations and handled failures.

## Run the project

Requirements: JDK 17 or newer. Maven Wrapper downloads Maven and dependencies on the first run, so internet access is needed then. No database, framework or API key is required.

macOS or Linux:

```bash
./mvnw clean test
./mvnw package
java -jar target/smart-delivery-1.0.0.jar
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package
java -jar target/smart-delivery-1.0.0.jar
```

If Maven is already installed, replace `./mvnw` with `mvn`. Open `pom.xml` as a Maven project in IntelliJ IDEA. Run `delivery.Main`, or right-click `RouteEngineTest` to run the tests.

## Architecture and Strategy Pattern

**Problem solved.** The company needs different definitions of a good route. Putting every algorithm in a large if/else chain would couple route planning to every scoring formula. Strategy gives each algorithm its own class and lets the client replace it at runtime.

**Context.** `RoutePlanner` holds a `RouteStrategy`. Its `plan` method only delegates to `strategy.selectRoute`. It contains no scoring formula, strategy-name switch or concrete-strategy type check.

**Strategy interface.** `RouteStrategy` declares `selectRoute` and `getStrategyName`. This implementation returns `RouteDecision` instead of just `Route`. That small change keeps the selected route, score, explanation and rejected-route reasons together without printing inside the algorithms.

**Concrete strategies.** `FastestRouteStrategy`, `CheapestRouteStrategy`, `EcoFriendlyRouteStrategy`, `BalancedRouteStrategy` and `DistanceFirstStrategy` provide different objectives. Their common parent `ScoredRouteStrategy` implements the shared filtering and minimum-selection loop. Each concrete class supplies `score`, `explain` and its name. This helper avoids copying constraint handling five times. The main pattern is Strategy; the shared base class also uses a template-style selection method.

**Other responsibilities.** `RouteRules` provides common final-time, total-cost and eligibility calculations. `StrategySelector` selects a strategy with an `EnumMap`. `StrategyComparison` runs a supplied list of strategies on the same request. `DemoData` contains eight sample routes, and `Main` prints results.

The code uses records, an enum, interfaces, inheritance, lists, a map and ordinary loops. It does not use streams, reflection, dependency injection frameworks or a graphical interface.

[UML class and sequence diagrams](docs/uml.md) correspond to these classes and method calls. GitHub renders the Mermaid diagrams directly.

## Domain model and demonstration data

`Location` stores a name and coordinates. `Route` stores locations, distance, estimated time, base cost, emissions, traffic delay, extra fees and availability. `DeliveryRequest` stores priority, budget, deadline and eco preference. Distances are kilometres, time is minutes, money is KZT and emissions are estimated kilograms of CO2. All routes in the demonstration connect the same warehouse and customer; the values are educational examples, not live routing data.

| Route | km | Base min | Traffic min | Base KZT | Fees KZT | CO2 kg | Available |
|---|---:|---:|---:|---:|---:|---:|---|
| R1 | 12 | 20 | 10 | 2800 | 300 | 4.5 | Yes |
| R2 | 15 | 22 | 2 | 3000 | 500 | 5.0 | Yes |
| R3 | 8 | 45 | 5 | 700 | 0 | 2.0 | Yes |
| R4 | 10 | 40 | 3 | 1600 | 0 | 0.5 | Yes |
| R5 | 6 | 35 | 8 | 1800 | 200 | 3.0 | Yes |
| R6 | 11 | 28 | 4 | 1700 | 100 | 1.5 | Yes |
| R7 | 5 | 10 | 0 | 200 | 0 | 0.1 | No |
| R8 | 9 | 55 | 10 | 500 | 0 | 1.0 | Yes |

## Formulas and hard constraints

All objectives are minimized. Equal scores keep the first candidate in input order, so results are deterministic for a given route list.

### Fastest

```text
finalTime = estimatedMinutes + trafficPenaltyMinutes
```

Traffic is an additional condition: R1 has a lower base time than R2, but its final time is 30 minutes versus R2's 24 minutes. The same final-time calculation is used when checking the deadline.

### Cheapest

```text
prioritySurcharge = 500 KZT for URGENT, otherwise 0
cost = baseCost + distanceKm * 100 + prioritySurcharge + additionalFees
```

For STANDARD, R3 costs `700 + 8 * 100 + 0 + 0 = 1500 KZT`. For URGENT, it costs 2000 KZT. The calculation is shared in `RouteRules` because every strategy must enforce the budget using the same total cost. Cheapest uses that total as its objective.

### Eco friendly

```text
ecoScore = carbonEmission * 0.8 + distanceKm * 0.2
```

R4 scores `0.5 * 0.8 + 10 * 0.2 = 2.4`. The coefficients are fixed educational scoring coefficients, not a physical CO2 conversion: the result is a comparison score, not kilograms. This formula belongs in EcoFriendlyRouteStrategy because environmental ranking is that algorithm's responsibility; the planner should not know environmental policy.

### Balanced

```text
normalizedValue = (value - minimum) / (maximum - minimum)
score = normalizedTime * timeWeight
      + normalizedCost * costWeight
      + normalizedDistance * distanceWeight
      + normalizedEmission * emissionWeight
```

Minima and maxima are taken only from feasible routes for the current request. If all values of a criterion are equal, its normalized value is zero. This also handles a single feasible route without dividing by zero.

Default weights are `0.40, 0.30, 0.10, 0.20`. The constructor rejects negative, non-finite or greater-than-one weights and rejects a sum that differs from 1.0 by more than `1e-9`. This tolerance handles ordinary floating-point rounding.

For STANDARD, budget 6000 and deadline 60, feasible routes are R1–R6. The ranges are time 24–50, cost 1500–5000, distance 6–15 and emissions 0.5–5.0. R6 has:

```text
0.4 * (32 - 24) / (50 - 24)
+ 0.3 * (2900 - 1500) / (5000 - 1500)
+ 0.1 * (11 - 6) / (15 - 6)
+ 0.2 * (1.5 - 0.5) / (5.0 - 0.5)
= 0.3430769231
```

### Fifth strategy

`DistanceFirstStrategy` minimizes distance among feasible routes. For the comparison request it selects R5 at 6 km. R7 is shorter but unavailable.

### Constraints and failures

Before any scoring, each route is checked for availability, total cost not exceeding the budget, and final time not exceeding the deadline. Equality at either limit is accepted. R7 is unavailable and R8 misses the standard 60-minute deadline. Neither can influence Balanced normalization or win any strategy.

If no route is feasible, `NoSuitableRouteException` contains every rejected route's reasons. An empty list produces the same explicit exception with an empty-list explanation. Missing required objects fail with `NullPointerException`; invalid numeric values fail with `IllegalArgumentException`. Costs, distances, emissions and fees must be finite and non-negative. Coordinates and time values are also validated. `Main` demonstrates empty input, an impossible request and invalid weights with try/catch.

## Runtime and automatic strategy selection

`Main.main` reuses one `RoutePlanner`: Fastest initially, then Cheapest, Eco and Balanced. There are exactly three explicit switches in that demonstration. The same planner is also reused for automatic selection.

| Request | Selected strategy | Example result |
|---|---|---|
| URGENT, budget 6000, deadline 30 | Fastest | R2 |
| ECONOMY, budget 2000, deadline 60 | Cheapest | R3 |
| ECO, budget 6000, deadline 60 | Eco | R4 |
| STANDARD, budget 6000, deadline 60 | Balanced | R6 |
| STANDARD with ecoPreferred true | Eco | R4 |

`ecoPreferred` overrides only STANDARD. Explicit URGENT and ECONOMY priorities remain stronger. `StrategySelector.register` can replace a priority mapping without changing the planner or selection loop.

**Strategy decides how to rank routes. StrategySelector decides which algorithm to use.** The map avoids a large strategy-selection switch. Small conditionals for surcharge, constraints or preference precedence are ordinary business rules, not a replacement for polymorphism.

Dynamic requests also affect one unchanged algorithm: Fastest chooses R2 with a 6000 KZT budget and R6 with a 3000 KZT budget, both with the same STANDARD priority and 60-minute deadline.

## Comparison and explanations

The comparison mode runs all five strategies on the exact same request and candidates. A score is meaningful within its strategy; a time score must not be compared numerically to a normalized Balanced score.

Actual output excerpt for STANDARD, budget 6000 KZT, deadline 60 minutes:

```text
Fastest -> R2 | time=24 min | cost=5000.00 KZT | distance=15.0 km | CO2=5.0 kg | score=24.0000
Cheapest -> R3 | time=50 min | cost=1500.00 KZT | distance=8.0 km | CO2=2.0 kg | score=1500.0000
Eco -> R4 | time=43 min | cost=2600.00 KZT | distance=10.0 km | CO2=0.5 kg | score=2.4000
Balanced -> R6 | time=32 min | cost=2900.00 KZT | distance=11.0 km | CO2=1.5 kg | score=0.3431
Distance first -> R5 | time=43 min | cost=2600.00 KZT | distance=6.0 km | CO2=3.0 kg | score=6.0000
```

Every decision includes the objective explanation and all hard-constraint rejection reasons. Feasible losing routes simply have no better score; they are not described as constraint violations. See [complete captured console output](docs/console-output.txt).

## Tests and results

The project uses JUnit Jupiter **5.11.4**. A verified build ran on JDK 25.0.1 with compiler release 17, using Maven 3.9.12. Source and bytecode target Java 17; the executed test environment was JDK 25, not a separate JDK 17 runtime.

```text
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

[Captured test result](docs/test-results.txt). Tests assert expected route IDs, formula values, exception contents, selection changes and boundary behavior. They cover all five algorithms, three runtime switches, all automatic mappings, eco precedence, extensible registration, comparison, availability, total-cost budget filtering, traffic-aware deadlines, exact limits, no feasible routes, empty lists, invalid weights, negative data, missing objects, ties, one-route normalization, zero values and immutable rejection lists.

Official test framework reference: [JUnit 5.11.4 User Guide](https://docs.junit.org/5.11.4/user-guide/).

## Adding a strategy and SOLID

To add a similar minimum-score algorithm, extend `ScoredRouteStrategy` and implement its name, score and explanation. Add an instance to `Main.allStrategies()` for comparison. Optionally register it for a priority through `StrategySelector.register`. An algorithm requiring a different selection process can implement `RouteStrategy` directly.

The fifth strategy was added after the four-strategy application compiled and ran. Its separate commit adds `DistanceFirstStrategy` and modifies only one existing class, `Main`, to register it for comparison. It changes zero lines in `RoutePlanner`, the interface, the selector or the original strategies. Inspect it with:

```bash
git log --oneline --reverse
```

To inspect that commit reliably:

```bash
git log --oneline --grep='Add fifth'
git show <commit-hash>
```

- **SRP:** RoutePlanner delegates; each strategy ranks; StrategySelector selects the algorithm; RouteRules applies common delivery rules; Main handles demonstration and output.
- **OCP:** adding DistanceFirstStrategy does not change Context or existing algorithms. Application wiring changes once so the new algorithm appears in comparison.
- **DIP:** RoutePlanner depends on RouteStrategy, an abstraction, rather than any concrete implementation.

## Strategy compared with Bridge

Strategy encapsulates interchangeable algorithms for one operation. Here, RoutePlanner changes its route-selection algorithm at runtime. Bridge separates two independently varying hierarchies, such as a notification type and a message delivery channel. Both often use composition, but their intent differs: algorithm replacement versus independent evolution of two dimensions. This comparison is conceptual because the previous assignment's implementation is not part of this repository.

## Limitations

The engine selects from precomputed candidates; it does not find paths in a road graph or optimize several deliveries together. All candidates are assumed to connect the intended endpoints, because DeliveryRequest does not carry locations. Time, prices and emissions are fixed demonstration estimates. Weather, prohibited deliveries and emission caps are not implemented; availability, budget and deadline are the implemented hard constraints.

Money uses double for simplicity; a production system should use integer minor units or BigDecimal. Eco coefficients are illustrative. Balanced recomputes ranges in a simple loop for each candidate, taking O(n²) time; for large lists, precompute ranges once. Min-max normalization means adding a feasible candidate may change earlier rankings. EnumMap permits mapping replacement, but a brand-new priority still requires an enum value. Comparison propagates the no-route exception; with the current shared constraints, all strategies fail on the same infeasible request. This is a small console application intended for a basic Java defense.

## Assignment checklist

| Requirement | Evidence |
|---|---|
| 1 Scenario | Delivery routes and request-specific optimization |
| 2 Core architecture | RoutePlanner, RouteStrategy, five concrete classes |
| 3 Domain model and 6–8 routes | Records and eight routes in DemoData |
| 4 Strategy interface | RouteStrategy returning justified RouteDecision |
| 5 Four meaningful strategies | Fastest, Cheapest, Eco, Balanced; formulas above |
| 6 Delegating Context | RoutePlanner.plan |
| 7 Three runtime switches | First section of Main.main |
| 8 Automatic selection | StrategySelector with extensible EnumMap |
| 9 Dynamic conditions | Multiple requests; same Fastest with lower budget |
| 10 Hard constraints | Availability, total budget, final deadline |
| 11 No valid route | NoSuitableRouteException with reasons |
| 12 Comparison | StrategyComparison and Main.allStrategies |
| 13 Explanation | RouteDecision.reason and rejectedRoutes |
| 14 Fifth strategy | Separate DistanceFirstStrategy commit |
| 15 Failure cases | Main failures and validation tests |
| 16 At least 15 JUnit 5 tests | 30 passing tests in RouteEngineTest |
| 17 Two UML diagrams | docs/uml.md |
| 18 Clean Code and SOLID | SRP, OCP, DIP explanations above |
| 19 At least 8 meaningful commits | Actual staged implementation history; git log |
| 20 README and report | This report, UML, captured console output and test results |

For defense preparation in Kazakh, see [Қорғауға дайындық](docs/defense-kz.md). Submit the public repository link and this README/report to Moodle as instructed by the course. Individual defense and live modification are performed by the student.
