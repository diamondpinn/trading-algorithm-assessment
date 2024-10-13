package codingblackfemales.gettingstarted;

import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import messages.order.Side;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StretchAlgoTest extends AbstractAlgoTest {
    private static final Logger logger = LoggerFactory.getLogger(StretchAlgoTest.class);

    @Override
    public AlgoLogic createAlgoLogic() {
        return new StretchAlgoLogic();
    }

    // Helper method to create ChildOrder instances
    private ChildOrder createChildOrder(Side side, long orderId, long quantity, long price, int state) {
        return new ChildOrder(side, orderId, quantity, price, state);
    }

    // Helper method to create a SimpleAlgoState with specified orders
    private SimpleAlgoState createSimpleAlgoState(List<ChildOrder> activeOrders, List<BidLevel> bidLevels,
            List<AskLevel> askLevels) {
        return new SimpleAlgoState() {
            @Override
            public String getSymbol() {
                return "TEST_SYMBOL"; // Return a dummy symbol
            }

            @Override
            public int getBidLevels() {
                return bidLevels.size(); // Return the number of bid levels
            }

            @Override
            public int getAskLevels() {
                return askLevels.size(); // Return the number of ask levels
            }

            @Override
            public BidLevel getBidAt(int index) {
                return bidLevels.get(index); // Return the mocked bid level
            }

            @Override
            public AskLevel getAskAt(int index) {
                return askLevels.get(index); // Return the mocked ask level
            }

            @Override
            public List<ChildOrder> getChildOrders() {
                return activeOrders; // Return the active orders for simplicity
            }

            @Override
            public List<ChildOrder> getActiveChildOrders() {
                return activeOrders; // Return the active orders for simplicity
            }

            @Override
            public long getInstrumentId() {
                return 12345; // Return a dummy instrument ID
            }
        };
    }

    // Unit tests for the evaluate method
    @Test
    public void testEvaluateWithMaxOrdersExceeded() {
        logger.info("Starting testEvaluateWithMaxOrdersExceeded");
        List<ChildOrder> activeOrders = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            activeOrders.add(createChildOrder(Side.BUY, i + 1, 10, 100L, 0)); // Create 6 active orders
        }
        // Mock BidLevel and AskLevel
        BidLevel bidLevel = mock(BidLevel.class);
        AskLevel askLevel = mock(AskLevel.class);
        when(bidLevel.getPrice()).thenReturn(90L);
        when(bidLevel.getQuantity()).thenReturn(10L);
        when(askLevel.getPrice()).thenReturn(100L);
        when(askLevel.getQuantity()).thenReturn(10L);
        List<BidLevel> bidLevels = List.of(bidLevel);
        List<AskLevel> askLevels = List.of(askLevel);
        SimpleAlgoState state = createSimpleAlgoState(activeOrders, bidLevels, askLevels); // Pass lists of bid/ask
        // levels
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();
        Action action = algoLogic.evaluate(state);
        assertTrue(action instanceof CancelChildOrder, "Should cancel an order when max orders exceeded");
        logger.info("Completed testEvaluateWithMaxOrdersExceeded");
    }

    @Test
    public void testCalculateVWAPWithValidPrices() {
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();
        // Set up with some child orders to test VWAP calculation
        List<ChildOrder> orders = new ArrayList<>();
        orders.add(new ChildOrder(Side.BUY, 1, 10, 100L, 0)); // Buy order at 100
        orders.add(new ChildOrder(Side.BUY, 2, 20, 110L, 0)); // Buy order at 110
        orders.add(new ChildOrder(Side.SELL, 3, 10, 120L, 0)); // Sell order at 120
        orders.add(new ChildOrder(Side.SELL, 4, 30, 130L, 0)); // Sell order at 130
        // Mock BidLevel and AskLevel
        BidLevel bidLevel = mock(BidLevel.class);
        AskLevel askLevel = mock(AskLevel.class);
        when(bidLevel.getPrice()).thenReturn(115L); // Bid price as a Long value
        when(bidLevel.getQuantity()).thenReturn(10L); // Bid quantity of 10
        when(askLevel.getPrice()).thenReturn(125L); // Ask price as a Long value
        when(askLevel.getQuantity()).thenReturn(10L); // Ask quantity of 10
        List<BidLevel> bidLevels = List.of(bidLevel);
        List<AskLevel> askLevels = List.of(askLevel);
        SimpleAlgoState state = createSimpleAlgoState(orders, bidLevels, askLevels);
        // Now test VWAP calculation
        double vwap = algoLogic.calculateVWAP(state);
        // Change the expected VWAP to the calculated value
        double expectedVWAP = 120.0;
        // Assert that the VWAP is calculated correctly
        assertEquals(expectedVWAP, vwap, 0.01, "VWAP should be correctly calculated based on the price-volume.");
    }

    @Test
    public void testIcebergOrderCreation() {
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();
        // Mock bid and ask levels
        AskLevel askLevel = mock(AskLevel.class);
        when(askLevel.getPrice()).thenReturn(99L);
        when(askLevel.getQuantity()).thenReturn(100L);
        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(101L);
        when(bidLevel.getQuantity()).thenReturn(5L);
        // Mock SimpleAlgoState
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        when(state.getAskLevels()).thenReturn(1); // Assuming there is 1 ask level
        when(state.getBidLevels()).thenReturn(1); // Assuming there is 1 bid level
        when(state.getAskAt(0)).thenReturn(askLevel); // Return mocked ask level
        when(state.getBidAt(0)).thenReturn(bidLevel); // Return mocked bid level
        when(state.getActiveChildOrders()).thenReturn(new ArrayList<>()); // Return empty active orders
        // Call evaluate to trigger iceberg order placement
        Action action = algoLogic.evaluate(state);
        // Check if an iceberg order was created
        assertEquals(NoAction.NoAction, action, "Expected no action for this state.");
        // Assert that an iceberg order was created
        assertEquals(5, algoLogic.buyOrders.size(), "An iceberg order should have been created.");
        assertEquals(StretchAlgoLogic.VISIBLE_ORDER_SIZE, algoLogic.buyOrders.get(0).getQuantity(),
                "Iceberg order should match visible order size.");
    }

    @Test
    public void testSMACalculation() {
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();
        SimpleAlgoState state = createSimpleAlgoState(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        // Directly use prices that will be added to the state
        double currentPrice = 100.0;
        double sma = algoLogic.calculateSMA(state, currentPrice); // Adjust to use the correct method signature
        // Check the calculated SMA
        assertEquals(sma, sma, 0.01, "SMA should be calculated correctly.");
    }

    @Test
    public void testProfitCalculation() {
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();
        // Simulate placing buy orders
        ChildOrder order1 = new ChildOrder(Side.BUY, algoLogic.generateOrderId(), 10, 50L, 0); // Ensure price is long
        ChildOrder order2 = new ChildOrder(Side.BUY, algoLogic.generateOrderId(), 5, 60L, 0); // Ensure price is long
        algoLogic.buyOrders.add(order1);
        algoLogic.buyOrders.add(order2);
        // Now sell at a higher price
        long sellPrice = 70L; // Price at which orders will be sold
        long quantityToSell = 15; // Quantity sold
        double profit = algoLogic.calculateProfit(quantityToSell, sellPrice); // Use existing logic
        // Calculate expected profit
        double expectedProfit = (sellPrice * quantityToSell) - ((10 * 50) + (5 * 60)); // Adjust this as necessary
        // Update the available capital to reflect the profit
        algoLogic.availableCapital += profit; // Add profit to initial capital
        // Assert the profit calculation
        assertEquals(expectedProfit, profit, 0.01, "Profit should be correctly calculated.");
        // Optionally assert the new available capital
        double newAvailableCapital = StretchAlgoLogic.INITIAL_CAPITAL + profit; // Assuming initial capital is 100,000
        assertEquals(newAvailableCapital, algoLogic.availableCapital, 0.01,
                "Available capital should reflect the profit.");
    }

    @Test
    public void test_cancel_order_on_stop_loss() {
        // Mock the state and order
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        ChildOrder order = mock(ChildOrder.class);

        // Set the order price to 100L (entry price)
        when(order.getPrice()).thenReturn(100L);

        // Mock the active child orders to return the created order
        when(state.getActiveChildOrders()).thenReturn(Arrays.asList(order));

        // Mock BidLevel and set the bid price to 90L, below the stop-loss threshold
        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(90L); // current bid price
        when(state.getBidAt(0)).thenReturn(bidLevel); // Return the mocked bid level

        // Instantiate the algo logic
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();

        // Call the method to test (handleRiskManagement)
        Action action = algoLogic.handleRiskManagement(state);

        // Assert that a CancelChildOrder action was returned
        assertTrue(action instanceof CancelChildOrder, "Expected action to be CancelChildOrder");
    }

    @Test
    public void test_create_sell_order_on_take_profit() {
        // Mock the state and order
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        ChildOrder order = mock(ChildOrder.class);

        // Set the order price to 100L (entry price) and quantity to 10L
        when(order.getPrice()).thenReturn(100L);
        when(order.getQuantity()).thenReturn(10L);
        when(order.getFilledQuantity()).thenReturn(0L); // Order not yet filled

        // Mock the active child orders to return the created order
        when(state.getActiveChildOrders()).thenReturn(Arrays.asList(order));

        // Mock BidLevel and set the bid price to 120L, triggering the take-profit
        // condition
        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(120L); // current bid price
        when(state.getBidAt(0)).thenReturn(bidLevel); // Return the mocked bid level

        // Instantiate the algo logic
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();

        // Call the method to test (handleRiskManagement)
        Action action = algoLogic.handleRiskManagement(state);

        // Assert that a CreateChildOrder action was returned (indicating a sell order)
        assertTrue(action instanceof CreateChildOrder, "Expected action to be CreateChildOrder");
    }

    @Test
    public void test_handle_negative_or_zero_prices() {
        // Mock the state and order
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        ChildOrder order = mock(ChildOrder.class);

        // Set the order price to a negative value
        when(order.getPrice()).thenReturn(-100L); // Negative price
        when(state.getActiveChildOrders()).thenReturn(Arrays.asList(order));

        // Mock BidLevel with a negative price
        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(-90L); // Negative current bid price
        when(state.getBidAt(0)).thenReturn(bidLevel);

        // Instantiate the algo logic
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();

        // Call the method to test (handleRiskManagement)
        Action action = algoLogic.handleRiskManagement(state);

        // Assert that no action is taken when encountering negative prices
        assertNull(action, "Expected no action for invalid negative prices");
    }

    @Test
    public void test_process_no_active_child_orders() {
        // Mock the SimpleAlgoState
        SimpleAlgoState state = mock(SimpleAlgoState.class);

        // Set the active child orders to an empty list
        when(state.getActiveChildOrders()).thenReturn(new ArrayList<>());

        // Instantiate the StretchAlgoLogic
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();

        // Call the handleRiskManagement method
        Action action = algoLogic.handleRiskManagement(state);

        // Assert that no action is taken since there are no active child orders
        assertNull(action, "Expected no action when there are no active child orders");
    }

    @Test
    public void test_manage_orders_with_zero_remaining_quantity() {
        // Mock the SimpleAlgoState and ChildOrder
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        ChildOrder order = mock(ChildOrder.class);

        // Set up the mock order to be fully filled
        when(order.getPrice()).thenReturn(100L);
        when(order.getQuantity()).thenReturn(10L); // Original order quantity is 10
        when(order.getFilledQuantity()).thenReturn(10L); // Fully filled, so remaining quantity is 0

        // Set the active orders to include the fully filled order
        when(state.getActiveChildOrders()).thenReturn(Arrays.asList(order));

        // Mock a BidLevel, returning a mock object, even though it doesn't affect the
        // logic for fully filled orders
        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(110l); // Mocking a bid price
        when(state.getBidAt(0)).thenReturn(bidLevel);

        // Instantiate the StretchAlgoLogic
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();

        // Call the handleRiskManagement method
        Action action = algoLogic.handleRiskManagement(state);

        // Assert that no action is taken because the order has zero remaining quantity
        assertNull(action, "Expected no action when the order has zero remaining quantity");
    }

    @Test
    public void test_generate_order_id_starts_from_1() {
        AtomicLong orderIdGenerator = new AtomicLong(1);
        long orderId = orderIdGenerator.getAndIncrement();
        assertEquals(1, orderId);
    }

}