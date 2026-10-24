package delivery;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class RouteEngineTest {
    private final Location origin = new Location("A", 0, 0);
    private final Location destination = new Location("B", 1, 1);
    private final DeliveryRequest standard = new DeliveryRequest(DeliveryPriority.STANDARD, 6000, 60, false);
    private Route route(String id, double km, int minutes, double base, double emissions, int traffic, double fees, boolean available) {
        return new Route(id, origin, destination, km, minutes, base, emissions, traffic, fees, available);
    }
    private BalancedRouteStrategy balanced() { return new BalancedRouteStrategy(.4, .3, .1, .2); }
    private String winner(RouteStrategy strategy) { return strategy.selectRoute(DemoData.routes(), standard).route().id(); }

    @Test void fastestUsesTrafficRatherThanBaseTime() {
        Route a = route("A", 1, 10, 100, 1, 20, 0, true);
        Route b = route("B", 1, 20, 100, 1, 0, 0, true);
        RouteDecision result = new FastestRouteStrategy().selectRoute(List.of(a, b), standard);
        assertEquals("B", result.route().id());
        assertEquals(20, result.score());
    }
    @Test void cheapestUsesTotalCostRatherThanBaseCost() {
        Route a = route("A", 10, 10, 100, 1, 0, 500, true);
        Route b = route("B", 1, 10, 500, 1, 0, 0, true);
        RouteDecision result = new CheapestRouteStrategy().selectRoute(List.of(a, b), standard);
        assertEquals("B", result.route().id());
        assertEquals(600, result.score());
    }
    @Test void urgentCostIncludesSurchargeAndFees() {
        Route a = route("A", 2, 10, 100, 1, 0, 50, true);
        DeliveryRequest urgent = new DeliveryRequest(DeliveryPriority.URGENT, 1000, 60, false);
        assertEquals(850, RouteRules.totalCost(a, urgent));
    }
    @Test void ecoCombinesEmissionAndDistance() {
        Route a = route("A", 10, 10, 100, 0.1, 0, 0, true);
        Route b = route("B", 2, 10, 100, 1, 0, 0, true);
        RouteDecision result = new EcoFriendlyRouteStrategy().selectRoute(List.of(a, b), standard);
        assertEquals("B", result.route().id());
        assertEquals(1.2, result.score(), 1e-9);
    }
    @Test void balancedHasExpectedNormalizedScore() {
        RouteDecision result = balanced().selectRoute(DemoData.routes(), standard);
        assertEquals("R6", result.route().id());
        // time 8/26, cost 1400/3500, distance 5/9, emissions 1/4.5
        assertEquals(.4*8/26 + .3*1400/3500 + .1*5/9 + .2*1/4.5, result.score(), 1e-9);
    }
    @Test void balancedWeightsCanChangeWinner() {
        assertEquals("R2", winner(new BalancedRouteStrategy(1,0,0,0)));
        assertEquals("R3", winner(new BalancedRouteStrategy(0,1,0,0)));
    }
    @Test void samePlannerSupportsThreeRuntimeSwitches() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        assertEquals("R2", planner.plan(DemoData.routes(), standard).route().id());
        planner.setStrategy(new CheapestRouteStrategy());
        assertEquals("R3", planner.plan(DemoData.routes(), standard).route().id());
        planner.setStrategy(new EcoFriendlyRouteStrategy());
        assertEquals("R4", planner.plan(DemoData.routes(), standard).route().id());
        planner.setStrategy(balanced());
        assertEquals("R6", planner.plan(DemoData.routes(), standard).route().id());
    }
    @Test void selectorMapsAllPriorities() {
        StrategySelector selector = new StrategySelector();
        assertEquals("Fastest", selector.select(new DeliveryRequest(DeliveryPriority.URGENT,6000,60,false)).getStrategyName());
        assertEquals("Cheapest", selector.select(new DeliveryRequest(DeliveryPriority.ECONOMY,6000,60,false)).getStrategyName());
        assertEquals("Eco", selector.select(new DeliveryRequest(DeliveryPriority.ECO,6000,60,false)).getStrategyName());
        assertEquals("Balanced", selector.select(standard).getStrategyName());
    }
    @Test void ecoPreferenceOverridesStandardOnly() {
        StrategySelector selector = new StrategySelector();
        assertEquals("Eco", selector.select(new DeliveryRequest(DeliveryPriority.STANDARD,6000,60,true)).getStrategyName());
        assertEquals("Fastest", selector.select(new DeliveryRequest(DeliveryPriority.URGENT,6000,60,true)).getStrategyName());
        assertEquals("Cheapest", selector.select(new DeliveryRequest(DeliveryPriority.ECONOMY,6000,60,true)).getStrategyName());
    }
    @Test void selectorRegistrationIsExtensible() {
        StrategySelector selector = new StrategySelector();
        selector.register(DeliveryPriority.STANDARD, new DistanceFirstStrategy());
        assertEquals("R5", winner(selector.select(standard)));
    }
    @Test void everyStrategyRejectsUnavailableRoute() {
        for (RouteStrategy strategy : Main.allStrategies()) {
            RouteDecision result = strategy.selectRoute(DemoData.routes(), standard);
            assertNotEquals("R7", result.route().id());
            assertTrue(result.rejectedRoutes().contains("R7: Route unavailable"));
        }
    }
    @Test void budgetRestrictionChangesFastestWinner() {
        DeliveryRequest limited = new DeliveryRequest(DeliveryPriority.STANDARD,3000,60,false);
        assertEquals("R6", new FastestRouteStrategy().selectRoute(DemoData.routes(), limited).route().id());
    }
    @Test void allStrategiesUseTotalCostForBudgetFiltering() {
        Route costly = route("A",1,1,100,0,0,900,true);
        Route valid = route("B",2,2,100,1,0,0,true);
        DeliveryRequest limited = new DeliveryRequest(DeliveryPriority.STANDARD,500,60,false);
        for (RouteStrategy strategy : Main.allStrategies()) {
            assertEquals("B", strategy.selectRoute(List.of(costly,valid),limited).route().id());
        }
    }
    @Test void deadlineIncludesTrafficPenalty() {
        Route a = route("A",1,10,0,0,20,0,true);
        Route b = route("B",2,20,100,1,0,0,true);
        DeliveryRequest limited = new DeliveryRequest(DeliveryPriority.STANDARD,6000,20,false);
        for (RouteStrategy strategy : Main.allStrategies()) {
            RouteDecision result = strategy.selectRoute(List.of(a,b),limited);
            assertEquals("B", result.route().id());
            assertTrue(result.rejectedRoutes().get(0).contains("deadline"));
        }
    }
    @Test void noValidRouteHasExplicitReasons() {
        DeliveryRequest impossible = new DeliveryRequest(DeliveryPriority.STANDARD,0,0,false);
        NoSuitableRouteException error = assertThrows(NoSuitableRouteException.class,
                () -> balanced().selectRoute(DemoData.routes(),impossible));
        assertEquals(8,error.getReasons().size());
        assertTrue(error.getMessage().contains("Budget exceeded"));
    }
    @Test void emptyListFailsExplicitly() {
        NoSuitableRouteException error = assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(List.of(),standard));
        assertTrue(error.getMessage().contains("empty"));
    }
    @Test void balancedRejectsInvalidWeightSum() {
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(.5,.5,.5,.5));
    }
    @Test void balancedRejectsNegativeAndNonFiniteWeights() {
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(-.1,.5,.3,.3));
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(Double.NaN,0,0,1));
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(Double.POSITIVE_INFINITY,0,0,0));
    }
    @Test void fifthStrategyChoosesShortestFeasibleRoute() { assertEquals("R5",winner(new DistanceFirstStrategy())); }
    @Test void comparisonRunsAllStrategiesOnSameRequest() {
        List<RouteDecision> results = new StrategyComparison().compare(Main.allStrategies(),DemoData.routes(),standard);
        assertEquals(5,results.size());
        assertEquals("R2",results.get(0).route().id());
        assertEquals("R3",results.get(1).route().id());
        assertEquals("R4",results.get(2).route().id());
        assertEquals("R6",results.get(3).route().id());
        assertEquals("R5",results.get(4).route().id());
    }
    @Test void exactBudgetAndDeadlineAreAccepted() {
        Route a=route("A",2,10,100,1,5,50,true);
        DeliveryRequest boundary=new DeliveryRequest(DeliveryPriority.STANDARD,350,15,false);
        for(RouteStrategy strategy:Main.allStrategies()) assertEquals(a,strategy.selectRoute(List.of(a),boundary).route());
    }
    @Test void balancedSingleRouteProducesZeroNotNaN() {
        Route a=route("A",2,10,100,1,0,0,true);
        assertEquals(0,balanced().selectRoute(List.of(a),standard).score());
    }
    @Test void tiesKeepFirstCandidate() {
        Route a=route("A",2,10,100,1,0,0,true);
        Route b=route("B",2,10,100,1,0,0,true);
        for(RouteStrategy strategy:Main.allStrategies()) assertEquals(a,strategy.selectRoute(List.of(a,b),standard).route());
    }
    @Test void negativeDomainValuesFail() {
        assertThrows(IllegalArgumentException.class,()->route("A",-1,10,100,1,0,0,true));
        assertThrows(IllegalArgumentException.class,()->route("A",1,10,-100,1,0,0,true));
        assertThrows(IllegalArgumentException.class,()->route("A",1,-10,100,1,0,0,true));
        assertThrows(IllegalArgumentException.class,()->new DeliveryRequest(DeliveryPriority.STANDARD,-1,60,false));
    }
    @Test void missingDataFailsClearly() {
        assertThrows(NullPointerException.class,()->new DeliveryRequest(null,100,60,false));
        assertThrows(NullPointerException.class,()->balanced().selectRoute(DemoData.routes(),null));
        assertThrows(NullPointerException.class,()->balanced().selectRoute(null,standard));
        assertThrows(NullPointerException.class,()->balanced().selectRoute(Arrays.asList((Route)null),standard));
    }
    @Test void plannerRejectsNullStrategyAndKeepsOldOne() {
        RoutePlanner planner=new RoutePlanner(new FastestRouteStrategy());
        assertThrows(NullPointerException.class,()->planner.setStrategy(null));
        assertEquals("R2",planner.plan(DemoData.routes(),standard).route().id());
    }
    @Test void rejectionListCannotBeChanged() {
        RouteDecision result=balanced().selectRoute(DemoData.routes(),standard);
        assertThrows(UnsupportedOperationException.class,()->result.rejectedRoutes().add("fake"));
    }
    @Test void balancedNormalizationExcludesRejectedOutlier() {
        Route a=route("A",1,10,100,1,0,0,true);
        Route b=route("B",2,20,200,2,0,0,true);
        Route outlier=route("X",1000,1000,100000,1000,0,0,false);
        RouteDecision first=balanced().selectRoute(List.of(a,b),standard);
        RouteDecision second=balanced().selectRoute(List.of(a,b,outlier),standard);
        assertEquals(first.route(),second.route());
        assertEquals(first.score(),second.score());
    }
    @Test void urgentSurchargeCanMakeRouteInfeasible() {
        Route a=route("A",1,10,100,1,0,0,true);
        DeliveryRequest urgent=new DeliveryRequest(DeliveryPriority.URGENT,600,60,false);
        assertThrows(NoSuitableRouteException.class,()->new CheapestRouteStrategy().selectRoute(List.of(a),urgent));
    }
    @Test void zeroCostTimeAndDistanceAreValidBoundaries() {
        Route a=route("A",0,0,0,0,0,0,true);
        DeliveryRequest zero=new DeliveryRequest(DeliveryPriority.STANDARD,0,0,false);
        for(RouteStrategy strategy:Main.allStrategies()) assertEquals(0,strategy.selectRoute(List.of(a),zero).score());
    }
}
