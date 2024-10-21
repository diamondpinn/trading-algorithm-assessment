package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import messages.marketdata.BookUpdateEncoder;
import messages.marketdata.InstrumentStatus;
import messages.marketdata.MessageHeaderEncoder;
import messages.marketdata.Source;
import messages.marketdata.Venue;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.nio.ByteBuffer;
import java.util.List;

public class MyAlgoBackTest extends AbstractAlgoBackTest {
    private static final Logger logger = LoggerFactory.getLogger(MyAlgoBackTest.class);
    private static final int MAX_ORDER_COUNT = 0;
    private final AlgoLogic algo = createAlgoLogic();

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();
    }

    @Test
    public void testStopLoss() throws Exception {

        logger.info("Starting testStopLoss");
        // Create market scenario with initial bid and ask prices
        long initialBidPrice = 90L;
        long initialAskPrice = 92L;
        UnsafeBuffer tickData = createTick(initialBidPrice, initialAskPrice);
        send(tickData);
        logger.info("Initial market tick sent with Bid: {}, Ask: {}", initialBidPrice, initialAskPrice);

        // Get the initial state
        var state = container.getState();
        assertTrue("Expected at least one child order", state.getChildOrders().size() > 0);
        logger.info("Child order detected in state");

        // Get the first child order
        ChildOrder filledOrder = state.getChildOrders().get(0);

        // Simulate the order being filled
        filledOrder.addFill(filledOrder.getQuantity(), filledOrder.getPrice());
        filledOrder.setState(OrderState.FILLED);
        logger.info("Order filled with Quantity: {}, Price: {}", filledOrder.getQuantity(), filledOrder.getPrice());

        // Simulate a market tick with a price that triggers the stop loss (2% below the
        // filled price)
        long stopLossPrice = filledOrder.getPrice() - (long) (filledOrder.getPrice() * 0.02);
        UnsafeBuffer stopLossTick = createTick(stopLossPrice, stopLossPrice + 2);
        send(stopLossTick);
        logger.info("Market tick sent with Stop Loss Price: {}", stopLossPrice);
        // Check if the order was canceled due to stop loss
        state = container.getState();
        List<ChildOrder> activeOrders = state.getActiveChildOrders();
        boolean orderCanceled = !activeOrders.contains(filledOrder);
        logger.info("Order stop-loss canceled: {}", orderCanceled);
        assertTrue("Order should be canceled due to stop-loss", orderCanceled);
        assertEquals("Order state should be CANCELED due to stop-loss", OrderState.CANCELLED, filledOrder.getState());
        logger.info("testStopLoss completed successfully");
    }

    @Test
    public void testTakeProfit() throws Exception {

        long initialBidPrice = 90L;
        long initialAskPrice = 92L;
        UnsafeBuffer tickData = createTick(initialBidPrice, initialAskPrice);
        send(tickData);
        var state = container.getState();
        assertTrue("Expected at least one child order", state.getChildOrders().size() > 0);

        // Get the first child order
        ChildOrder filledOrder = state.getChildOrders().get(0);
        // Simulate the order being filled
        filledOrder.addFill(filledOrder.getQuantity(), filledOrder.getPrice());
        filledOrder.setState(OrderState.FILLED);

        // Simulate a market tick with a price that triggers the take profit (2% above
        // the filled price)
        long takeProfitPrice = filledOrder.getPrice() + (long) (filledOrder.getPrice() * 0.02);
        UnsafeBuffer takeProfitTick = createTick(takeProfitPrice, takeProfitPrice + 2);
        send(takeProfitTick);

        // Check if the order was canceled due to take profit
        state = container.getState();
        List<ChildOrder> activeOrders = state.getActiveChildOrders();
        boolean orderCanceled = !activeOrders.contains(filledOrder);
        assertTrue("Order should be canceled due to take-profit", orderCanceled);
        assertEquals("Order state should be CANCELED due to take-profit", OrderState.CANCELLED, filledOrder.getState());
    }

    @Test
    public void testMaxOrderLimit() throws Exception {
        // Create initial market scenario
        long initialBidPrice = 90L;
        long initialAskPrice = 92L;
        UnsafeBuffer tickData = createTick(initialBidPrice, initialAskPrice);
        send(tickData);
        // Create initial orders until we reach the maximum limit
        for (int i = 0; i < MyAlgoLogic.MAX_ORDER_COUNT; i++) {
            UnsafeBuffer additionalTickData = createTick(initialBidPrice - i, initialAskPrice - i);
            send(additionalTickData);
        }
        // Check the state after reaching maximum order count
        var state = container.getState();
        List<ChildOrder> orders = state.getChildOrders();
        assertEquals("Total child orders should be equal to MAX_ORDER_COUNT", MyAlgoLogic.MAX_ORDER_COUNT,
                orders.size());
        // Attempt to send another tick and expect NoAction since max limit reached
        UnsafeBuffer newTickData = createTick(initialBidPrice - MyAlgoLogic.MAX_ORDER_COUNT,
                initialAskPrice - MyAlgoLogic.MAX_ORDER_COUNT);
        send(newTickData);
        // Evaluate the algorithm logic with the new tick data
        Action action = algo.evaluate(state);
        assertTrue("Expected NoAction since max order limit reached", action instanceof NoAction);
    }

    @Test
    public void testMultipleActiveOrders() throws Exception {
        // Create initial market scenario
        long initialBidPrice = 100L;
        long initialAskPrice = 102L;
        UnsafeBuffer tickData = createTick(initialBidPrice, initialAskPrice);
        send(tickData);
        // Ensure at least one order is created
        var state = container.getState();
        assertTrue("Expected at least one child order", state.getChildOrders().size() > 0);
        // Simulate placing additional orders until MAX_ORDER_COUNT is reached
        for (int i = 0; i < MAX_ORDER_COUNT; i++) {
            UnsafeBuffer additionalTickData = createTick(initialBidPrice - i, initialAskPrice - i);
            send(additionalTickData);
        }
        // Verify that the number of active orders matches the MAX_ORDER_COUNT
        state = container.getState();
        List<ChildOrder> activeOrders = state.getActiveChildOrders();
        assertEquals("Should have reached maximum order count", MAX_ORDER_COUNT, activeOrders.size());
    }

    protected UnsafeBuffer createTick4(long bidPrice, long askPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        // Write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        // Set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        encoder.bidBookCount(9)
                .next().price(bidPrice).size(100L) // Far touch bid
                .next().price(bidPrice - 1).size(200L)
                .next().price(bidPrice - 2).size(150L)
                .next().price(bidPrice - 3).size(250L)
                .next().price(bidPrice - 4).size(300L)
                .next().price(bidPrice - 5).size(350L)
                .next().price(bidPrice - 6).size(400L)
                .next().price(bidPrice - 7).size(450L)
                .next().price(bidPrice - 8).size(500L);

        encoder.askBookCount(9)
                .next().price(askPrice).size(100L) // Far touch ask
                .next().price(askPrice + 1).size(200L)
                .next().price(askPrice + 2).size(150L)
                .next().price(askPrice + 3).size(250L)
                .next().price(askPrice + 4).size(300L)
                .next().price(askPrice + 5).size(350L)
                .next().price(askPrice + 6).size(400L)
                .next().price(askPrice + 7).size(450L)
                .next().price(askPrice + 8).size(500L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void testCreateOrderWhenNoActiveOrdersAndBidAvailable() throws Exception {
        logger.info("Starting testCreateOrderWhenNoActiveOrdersAndBidAvailable");

        // Define specific ask and bid prices
        long askPrice = 100L;
        long bidPrice = 98L;
        // prices
        UnsafeBuffer tickData = createTick4(askPrice, bidPrice);
        send(tickData);

        SimpleAlgoState state = container.getState();

        // Ensure that at least one child order was created
        assertTrue("Expected at least one child order", state.getChildOrders().size() > 0);
        logger.info("Child order detected in state");

        SimpleAlgoState currentState = container.getState();
        List<ChildOrder> childOrders = currentState.getChildOrders();

        assertEquals(8, childOrders.size()); // Ensuring all 8 orders are created as expected

    }

    protected UnsafeBuffer createTick5(long bidPrice, long askPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        // Write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        // Set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        encoder.bidBookCount(9)
                .next().price(bidPrice).size(100L) // Far touch bid
                .next().price(bidPrice - 1).size(200L)
                .next().price(bidPrice - 2).size(150L)
                .next().price(bidPrice - 3).size(250L)
                .next().price(bidPrice - 4).size(300L)
                .next().price(bidPrice - 5).size(350L)
                .next().price(bidPrice - 6).size(400L)
                .next().price(bidPrice - 7).size(450L)
                .next().price(bidPrice - 8).size(500L);

        encoder.askBookCount(9)
                .next().price(askPrice).size(100L) // Far touch ask
                .next().price(askPrice + 1).size(200L)
                .next().price(askPrice + 2).size(150L)
                .next().price(askPrice + 3).size(250L)
                .next().price(askPrice + 4).size(300L)
                .next().price(askPrice + 5).size(350L)
                .next().price(askPrice + 6).size(400L)
                .next().price(askPrice + 7).size(450L)
                .next().price(askPrice + 8).size(500L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void testFilledOrders() throws Exception {
        logger.info("Starting testFilledOrders");

        // Define the specific ask and bid prices
        long askPrice = 100L;
        long bidPrice = 98L;

        UnsafeBuffer tickData = createTick5(bidPrice, askPrice);
        send(tickData);

        SimpleAlgoState state = container.getState();

        // Ensure that at least one child order was created
        assertTrue("Expected at least one child order", state.getChildOrders().size() > 0);
        logger.info("Child order detected in state");

        // Iterate through child orders and simulate filling them
        List<ChildOrder> childOrders = state.getChildOrders();
        for (ChildOrder order : childOrders) {
            if (order.getState() == OrderState.PENDING) {
                // Simulate filling the order
                long orderQuantity = order.getQuantity();
                long filledPrice = order.getPrice();
                long filledQuantity = orderQuantity;
                // Mark the order as filled
                order.addFill(filledQuantity, filledPrice);
                order.setState(OrderState.FILLED);

                logger.info("Order filled: " + filledQuantity + " @ " + filledPrice);

                // Assert that the filled quantity matches the order quantity
                assertEquals("Expected full order to be filled", orderQuantity, order.getFilledQuantity());
            }
        }

        // Log the filled orders' quantity and price
        logger.info("Filled orders summary:");
        for (ChildOrder filledOrder : childOrders) {
            if (filledOrder.getState() == OrderState.FILLED) {
                logger.info("Order filled with quantity: " + filledOrder.getFilledQuantity() +
                        " at price: " + filledOrder.getPrice());
            }
        }
    }

}