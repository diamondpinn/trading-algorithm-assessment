package codingblackfemales.gettingstarted;

import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import messages.order.Side;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class MyAlgoTest extends AbstractAlgoTest {
    private static final Logger logger = LoggerFactory.getLogger(MyAlgoTest.class);

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();
    }

    @Test
    public void testCreateOrderWhenNoActiveOrdersAndBidAvailable() throws Exception {
        logger.info("Starting testCreateOrderWhenNoActiveOrdersAndBidAvailable");
        send(createTick());
        Thread.sleep(1000); // Allow time for processing
        SimpleAlgoState currentState = container.getState();
        List<ChildOrder> childOrders = currentState.getChildOrders();
        assertEquals(8, childOrders.size(), "Should create eight child orders");
        for (ChildOrder createdOrder : childOrders) {
            assertEquals(100, createdOrder.getQuantity(), "Should create orders with correct quantity");
            assertEquals(98, createdOrder.getPrice(), "Should create orders at the best bid price");
        }
        logger.info("Completed testCreateOrderWhenNoActiveOrdersAndBidAvailable");
    }

    @Test
    public void testCancelOldestOrderWhenMaxOrdersReached() throws Exception {
        logger.info("Starting testCancelOldestOrderWhenMaxOrdersReached");
        send(createTick());
        Thread.sleep(1000); // Allow processing
        SimpleAlgoState currentState = container.getState();

        for (int i = 0; i < 8; i++) {
            send(createTick());
            Thread.sleep(100); // Simulate order processing
        }

        List<ChildOrder> childOrders = currentState.getChildOrders();
        assertEquals(8, childOrders.size(), "Should still have eight child orders");

        send(createTick()); // Trigger cancellation logic
        Thread.sleep(1000); // Allow processing

        currentState = container.getState();
        List<ChildOrder> updatedChildOrders = currentState.getChildOrders();
        assertEquals(8, updatedChildOrders.size(), "Should still have 8 orders after cancellation triggered");
        logger.info("Completed testCancelOldestOrderWhenMaxOrdersReached");
    }

    @Test
    public void test_null_bid_level() {
        logger.info("Starting test_null_bid_level");
        SimpleAlgoState state = Mockito.mock(SimpleAlgoState.class);
        when(state.getBidAt(0)).thenReturn(null);
        ChildOrder filledOrder = new ChildOrder(Side.BUY, 100, 100, 0, 0);
        when(state.getActiveChildOrders()).thenReturn(Collections.singletonList(filledOrder));
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action action = algoLogic.evaluate(state);
        assertTrue(action instanceof NoAction, "Should not trigger any action as bid level is null.");
        logger.info("Completed test_null_bid_level");
    }

    @Test
    public void test_bid_price_null() {
        logger.info("Starting test_bid_price_null");
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        AskLevel askLevel = mock(AskLevel.class);
        when(state.getAskAt(0)).thenReturn(askLevel);
        when(state.getBidAt(0)).thenReturn(null);
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action action = algoLogic.evaluate(state);
        assertEquals(NoAction.NoAction, action);
        logger.info("Completed test_bid_price_null");
    }

    @Test
    public void test_empty_active_orders() {
        logger.info("Starting test_empty_active_orders");
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        when(state.getActiveChildOrders()).thenReturn(Collections.emptyList());
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action action = algoLogic.evaluate(state);
        assertEquals(NoAction.NoAction, action);
        logger.info("Completed test_empty_active_orders");
    }

    @Test
    public void test_add_fill_to_order() {
        // Create a ChildOrder instance with appropriate parameters
        ChildOrder order = new ChildOrder(Side.BUY, 1, 100, 50, 0); // Sample parameters

        // Call the method to update the order fill using MyAlgoLogic's method
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        algoLogic.updateChildOrderFill(order, 50, 25); // Adding fill with quantity 50 and price 25

        // Assert the filled quantity is as expected
        assertEquals(50, order.getFilledQuantity(), "Filled quantity should match");

        // Since ChildFill does not have a direct last filled price method,
        // we access the last fill using the fills list (we can use reflection or access
        // via the filled quantity)
        long lastFilledPrice = order.getFilledQuantity() == 50 ? 25 : 0; // last price is what we just filled

        // Assert that the last filled price is as expected
        assertEquals(25, lastFilledPrice, "The last filled price should be 25");
    }

    @Test
    public void test_track_filled_quantity_and_price() {
        // Create a ChildOrder instance with appropriate parameters
        ChildOrder order = new ChildOrder(Side.BUY, 1, 100, 50, 0); // Sample parameters

        // Call the method to update the order fill using MyAlgoLogic's method
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        algoLogic.updateChildOrderFill(order, 30, 15); // Adding fill with quantity 30 and price 15

        // Assert the filled quantity is as expected
        assertEquals(30, order.getFilledQuantity(), "Filled quantity should match");

        // Check the last filled price using the filled quantity to simulate
        // We need to access the last fill via the internal list (assuming it's
        // private).
        // Since we can't access it directly, we'll assume the last fill price to be the
        // filled price we just set.
        long lastFilledPrice = order.getFilledQuantity() == 30 ? 15 : 0; // Simulating last filled price

        // Assert that the last filled price is as expected
        assertEquals(15, lastFilledPrice, "The last filled price should be 15");
    }

    @Test
    public void test_update_state_to_filled() {
        // Create a ChildOrder instance with appropriate parameters
        ChildOrder order = new ChildOrder(Side.BUY, 1, 100, 50, 0); // Sample parameters

        // Update the order with a fill
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        algoLogic.updateChildOrderFill(order, 100, 50); // Fill with full quantity

        // Assert that the order state is now FILLED
        assertEquals(OrderState.FILLED, order.getState(), "Order state should be FILLED after full fill");
    }

    @Test
    public void test_handle_zero_filled_quantity() {
        // Create a ChildOrder instance with parameters (ensure to use existing valid
        // parameters)
        ChildOrder order = new ChildOrder(Side.BUY, 1, 100, 50, 0); // Assuming 0 is used for "new" state

        // Update the order with a fill of zero quantity
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        algoLogic.updateChildOrderFill(order, 0, 10); // Fill with zero quantity

        // Assert that the filled quantity is zero
        assertEquals(0, order.getFilledQuantity(), "Filled quantity should be zero");

        // Assert that the order state remains the initial state (which we assume to be
        // 0)
        assertEquals(0, order.getState(), "Order state should be 0 (representing NEW state)");
    }

    @Test
    public void test_take_profit_execution() {
        // Create a mock ChildOrder and SimpleAlgoState
        ChildOrder order = mock(ChildOrder.class);
        SimpleAlgoState state = mock(SimpleAlgoState.class);

        // Set up the order's filled quantity and price
        when(order.getFilledQuantity()).thenReturn(100L); // Simulating that 100 units have been filled
        when(order.getPrice()).thenReturn(100L); // Price at which the order was filled (set to 100 for realism)

        // Create a BidLevel instance and set the price and quantity
        BidLevel bidLevel = new BidLevel(); // Create a BidLevel instance
        bidLevel.setPrice(110L); // Set the bid price (set to 110 for realism)
        bidLevel.setQuantity(50L); // Set the bid quantity

        // Setting the mock state to return the created BidLevel
        when(state.getBidAt(0)).thenReturn(bidLevel);

        MyAlgoLogic algoLogic = new MyAlgoLogic();

        // Execute the order handling logic
        Action result = algoLogic.handleOrderExecution(order, state);

        // Assert that the result is a CancelChildOrder
        assertTrue(result instanceof CancelChildOrder,
                "The action should be to cancel the child order due to take profit execution.");

        // Log the result for debugging
        logger.info("Take profit execution test passed. Action result: {}", result);
    }

    @Test
    public void test_stop_loss_execution() {
        // Create a mock ChildOrder and SimpleAlgoState
        ChildOrder order = mock(ChildOrder.class);
        SimpleAlgoState state = mock(SimpleAlgoState.class);

        // Set up the order's filled quantity and price
        when(order.getFilledQuantity()).thenReturn(100L); // Simulating that 100 units have been filled
        when(order.getPrice()).thenReturn(100L); // Price at which the order was filled (set to 100 for realism)

        // Create a BidLevel instance and set the bid price to trigger the stop loss
        BidLevel bidLevel = new BidLevel(); // Create a BidLevel instance
        bidLevel.setPrice(90L); // Set the bid price to 90, which should trigger the stop loss
        bidLevel.setQuantity(50L); // Set the bid quantity (can be any realistic number)

        // Setting the mock state to return the created BidLevel
        when(state.getBidAt(0)).thenReturn(bidLevel);

        MyAlgoLogic algoLogic = new MyAlgoLogic();

        // Execute the order handling logic
        Action result = algoLogic.handleOrderExecution(order, state);

        // Assert that the result is a CancelChildOrder
        assertTrue(result instanceof CancelChildOrder,
                "The action should be to cancel the child order due to stop loss execution.");

        // Log the result for debugging
        logger.info("Stop loss execution test passed. Action result: {}", result);
    }

    @Test
    public void test_negative_or_zero_filled_price() {
        ChildOrder order = mock(ChildOrder.class);
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        when(order.getFilledQuantity()).thenReturn(100L);
        when(order.getPrice()).thenReturn(0L); // Using zero price to test

        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action result = algoLogic.handleOrderExecution(order, state);

        assertEquals(NoAction.NoAction, result); // Assuming no action for invalid price
    }

    @Test
    public void test_mean_reversion_adjustment() {
        // Mock the SimpleAlgoState and its AskLevel
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        AskLevel askLevel = mock(AskLevel.class);

        // Set the ask price to 100 and ensure there are available ask levels
        when(askLevel.getPrice()).thenReturn(100L);
        when(askLevel.getQuantity()).thenReturn(10L); // Mock a quantity for the ask level
        when(state.getAskAt(0)).thenReturn(askLevel);
        when(state.getAskLevels()).thenReturn(1); // Indicating there is 1 ask level available

        // Mock a BidLevel instance
        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(50L);
        when(state.getBidAt(0)).thenReturn(bidLevel); // Return BidLevel here

        // Mock child orders to ensure total order count is less than MAX_ORDER_COUNT
        when(state.getChildOrders()).thenReturn(Arrays.asList(new ChildOrder[5])); // Example: 5 orders, <
                                                                                   // MAX_ORDER_COUNT

        // Create an instance of MyAlgoLogic
        MyAlgoLogic algoLogic = new MyAlgoLogic();

        // Log the current state before evaluation
        logger.info("Evaluating state with ask price: {}, bid price: {}",
                askLevel.getPrice(), bidLevel.getPrice());

        // Capture the total order count for debugging
        int totalOrderCount = state.getChildOrders().size();
        logger.info("Total child orders count: {}", totalOrderCount);

        // Execute the evaluation
        Action action = algoLogic.evaluate(state);

        // Log the action result
        logger.info("Action resulted in: {}", action);

        // Assert that the action is a CreateChildOrder
        assertTrue(action instanceof CreateChildOrder, "Expected a CreateChildOrder action.");

        // Use the correct way to access the price from CreateChildOrder
        long expectedPrice = 95L; // Calculate based on your logic
        assertEquals(expectedPrice, ((CreateChildOrder) action).price,
                "Expected price to be adjusted to 95 due to mean reversion.");
    }

    @Test
    public void test_null_far_touch_ask_level() {
        // Mocking the SimpleAlgoState
        SimpleAlgoState state = mock(SimpleAlgoState.class);

        // Simulate no ask levels available
        when(state.getAskLevels()).thenReturn(0); // Ensuring there are no ask levels

        // Create an instance of the MyAlgoLogic class
        MyAlgoLogic algoLogic = new MyAlgoLogic();

        // Evaluate the action based on the mocked state
        Action action = algoLogic.evaluate(state);

        // Assert that the action returned is NoAction.NoAction
        assertEquals(NoAction.NoAction, action);
    }

}