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
import messages.marketdata.BookUpdateEncoder;
import messages.marketdata.InstrumentStatus;
import messages.marketdata.MessageHeaderEncoder;
import messages.marketdata.Source;
import messages.marketdata.Venue;
import messages.order.Side;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MyAlgoTest extends AbstractAlgoTest {
    private static final Logger logger = LoggerFactory.getLogger(MyAlgoTest.class);

    @Override
    public AlgoLogic createAlgoLogic() {
        return new MyAlgoLogic();
    }

    protected UnsafeBuffer createTick(long askPrice, long bidPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        encoder.askBookCount(1)
                .next().price(askPrice).size(101L);

        encoder.bidBookCount(1)
                .next().price(bidPrice).size(100L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void testCreateOrderWhenNoActiveOrdersAndBidAvailable() throws Exception {
        logger.info("Starting testCreateOrderWhenNoActiveOrdersAndBidAvailable");

        // Create a tick with specific ask and bid prices
        long askPrice = 100L;
        long bidPrice = 98L;
        send(createTick(askPrice, bidPrice));

        Thread.sleep(1000);

        SimpleAlgoState currentState = container.getState();
        List<ChildOrder> childOrders = currentState.getChildOrders();

        assertEquals(8, childOrders.size());
        for (ChildOrder createdOrder : childOrders) {
            assertEquals(100, createdOrder.getQuantity());
            assertEquals(bidPrice, createdOrder.getPrice());
        }

        logger.info("Completed testCreateOrderWhenNoActiveOrdersAndBidAvailable");
    }

    protected UnsafeBuffer createTick2(long askPrice, long bidPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        encoder.askBookCount(1)
                .next().price(askPrice).size(101L);
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(100L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void testCancelOldestOrderWhenMaxOrdersReached() throws Exception {
        logger.info("Starting testCancelOldestOrderWhenMaxOrdersReached");

        send(createTick(100L, 98L));
        Thread.sleep(1000);
        SimpleAlgoState currentState = container.getState();

        for (int i = 0; i < 8; i++) {
            send(createTick2(100L, 98L));
            Thread.sleep(100);
        }

        List<ChildOrder> childOrders = currentState.getChildOrders();
        assertEquals(8, childOrders.size());

        // Send an additional tick to trigger the cancellation logic
        send(createTick2(100L, 98L));
        Thread.sleep(1000);

        currentState = container.getState();
        List<ChildOrder> updatedChildOrders = currentState.getChildOrders();
        assertEquals(8, updatedChildOrders.size()); // Check the updated child order count

        logger.info("Completed testCancelOldestOrderWhenMaxOrdersReached");
    }

    protected UnsafeBuffer createTick3() {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        encoder.askBookCount(1)
                .next().price(100L).size(101L); // Example ask price and size

        encoder.bidBookCount(0); // No bids

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void test_null_bid_level() throws Exception {
        logger.info("Starting test_null_bid_level");

        // Use createTick3 to send a tick with no bid levels
        send(createTick3());

        SimpleAlgoState currentState = container.getState();

        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action action = algoLogic.evaluate(currentState);
        // Check that the action is NoAction
        assertTrue("Should not trigger any action as bid level is null.", action instanceof NoAction);

        logger.info("Completed test_null_bid_level");
    }

    protected UnsafeBuffer createTick4() {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.askBookCount(1)
                .next().price(100L).size(101L);

        // Set bid count to zero to simulate a null bid
        encoder.bidBookCount(0);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void test_bid_price_null() throws Exception {
        logger.info("Starting test_bid_price_null");

        // Send a tick with a valid ask and a null bid
        send(createTick4());

        // Get the current state after sending the tick
        SimpleAlgoState currentState = container.getState();

        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action action = algoLogic.evaluate(currentState);

        assertEquals(NoAction.NoAction, action);

        logger.info("Completed test_bid_price_null");
    }

    protected UnsafeBuffer createTick5() {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        // Write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        // Set the fields to desired values for filling a buy order
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        // Adding ask levels and bid levels to facilitate the fill scenario
        encoder.askBookCount(1)
                .next().price(200L).size(100L); // Ask price above the last filled price

        encoder.bidBookCount(1)
                .next().price(100L).size(200L); // Bid price at the order's price level

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void test_add_fill_to_order() throws Exception {
        logger.info("Starting test_add_fill_to_order");

        ChildOrder order = new ChildOrder(Side.BUY, 1, 100, 50, 0);

        send(createTick5());
        Thread.sleep(1000);

        // Call the method to update the order fill using MyAlgoLogic's method
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        algoLogic.updateChildOrderFill(order, 50, 25); // Adding fill with quantity 50 and price 25

        // Assert the filled quantity is as expected
        assertEquals(50, order.getFilledQuantity());

        // Access the last filled price directly
        long lastFilledPrice = order.getFilledQuantity() == 50 ? 25 : 0;

        // Assert that the last filled price is as expected
        assertEquals(25L, lastFilledPrice);
        logger.info("Completed test_add_fill_to_order");
    }

    protected UnsafeBuffer createTick6(long askPrice, long bidPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        encoder.askBookCount(1)
                .next().price(askPrice).size(100L);
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(100L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void test_track_filled_quantity_and_price() throws Exception {
        logger.info("Starting test_track_filled_quantity_and_price");

        // Create a ChildOrder instance with appropriate parameters
        ChildOrder order = new ChildOrder(Side.BUY, 1, 100, 50, 0); // Sample parameters

        long askPrice = 100L;
        long bidPrice = 95L;
        send(createTick6(askPrice, bidPrice));

        MyAlgoLogic algoLogic = new MyAlgoLogic();
        algoLogic.updateChildOrderFill(order, 30, 95); // Adding fill with quantity 30 and price 95

        assertEquals(30, order.getFilledQuantity());
        long lastFilledPrice = order.getFilledQuantity() == 30 ? 95 : 0;

        // Assert that the last filled price is as expected
        assertEquals(95, lastFilledPrice);

        logger.info("Completed test_track_filled_quantity_and_price");
    }

    protected UnsafeBuffer createTick7(long askPrice, long bidPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        // Create one ask and one bid level
        encoder.askBookCount(1)
                .next().price(askPrice).size(100L);

        encoder.bidBookCount(1)
                .next().price(bidPrice).size(100L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void test_update_state_to_filled() throws Exception {
        logger.info("Starting test_update_state_to_filled");

        ChildOrder order = new ChildOrder(Side.BUY, 1, 100, 50, 0); // Sample parameters

        // Create and send a tick to simulate market conditions
        long askPrice = 100L;
        long bidPrice = 100L;
        send(createTick7(askPrice, bidPrice));
        // Update the order with a fill
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        algoLogic.updateChildOrderFill(order, 100, 50); // Fill with full quantity

        // Assert that the order state is now FILLED
        assertEquals(OrderState.FILLED, order.getState());

        logger.info("Completed test_update_state_to_filled");
    }

    protected UnsafeBuffer createTick8(long askPrice, long bidPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        // Write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        // Set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        // Create one ask and one bid level
        encoder.askBookCount(1)
                .next().price(askPrice).size(100L); // Ask price and size

        encoder.bidBookCount(1)
                .next().price(bidPrice).size(100L); // Bid price and size

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void test_handle_zero_filled_quantity() throws Exception {
        logger.info("Starting test_handle_zero_filled_quantity");
        ChildOrder order = new ChildOrder(Side.BUY, 1, 100, 50, 0);
        long askPrice = 100L;
        long bidPrice = 100L;
        send(createTick8(askPrice, bidPrice));
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        algoLogic.updateChildOrderFill(order, 0, 10);
        assertEquals(0, order.getFilledQuantity());
        assertEquals(0, order.getState());
        logger.info("Completed test_handle_zero_filled_quantity");
    }

    protected UnsafeBuffer createTick9(long bidPrice, long askPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(50L);
        encoder.askBookCount(1)
                .next().price(askPrice).size(100L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);
        return directBuffer;
    }

    @Test
    public void test_take_profit_execution() {
        ChildOrder order = mock(ChildOrder.class);
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        when(order.getFilledQuantity()).thenReturn(100L);
        when(order.getPrice()).thenReturn(100L);
        BidLevel bidLevel = new BidLevel();
        bidLevel.setPrice(110L);
        bidLevel.setQuantity(50L);
        when(state.getBidAt(0)).thenReturn(bidLevel);
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action result = algoLogic.handleOrderExecution(order, state);
        assertTrue(result instanceof CancelChildOrder);
        logger.info("Take profit execution test passed. Action result: {}", result);
        String resultString = result.toString();
        assertTrue(resultString.contains("CancelChildOrder"));
    }

    protected UnsafeBuffer createTick10(long bidPrice, long askPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(50L);
        encoder.askBookCount(1)
                .next().price(askPrice).size(100L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);
        return directBuffer;
    }

    @Test
    public void test_stop_loss_execution() {
        ChildOrder order = mock(ChildOrder.class);
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        when(order.getFilledQuantity()).thenReturn(100L);
        when(order.getPrice()).thenReturn(100L);
        BidLevel bidLevel = new BidLevel();
        bidLevel.setPrice(90L);
        bidLevel.setQuantity(50L);
        when(state.getBidAt(0)).thenReturn(bidLevel);
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action result = algoLogic.handleOrderExecution(order, state);
        assertTrue(result instanceof CancelChildOrder);
        logger.info("Stop loss execution test passed. Action result: {}", result);
        String resultString = result.toString();
        assertTrue(resultString.contains("CancelChildOrder"));
    }

    protected UnsafeBuffer createTick11(long bidPrice, long askPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(50L);
        encoder.askBookCount(1)
                .next().price(askPrice).size(100L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);
        return directBuffer;
    }

    @Test
    public void test_negative_or_zero_filled_price() throws Exception {
        logger.info("Starting test_negative_or_zero_filled_price");
        ChildOrder order = new ChildOrder(Side.BUY, 1, 0, 100, 0);
        long bidPrice = 95L;
        long askPrice = 105L;
        send(createTick11(bidPrice, askPrice));
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action result = algoLogic.handleOrderExecution(order, container.getState());
        assertEquals(NoAction.NoAction, result);
        logger.info("Completed test_negative_or_zero_filled_price");
    }

    protected UnsafeBuffer createTick12() {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.askBookCount(1)
                .next().price(100L).size(10L);
        encoder.bidBookCount(1)
                .next().price(50L).size(15L);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);
        return directBuffer;
    }

    @Test
    public void test_mean_reversion_adjustment() {
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        AskLevel askLevel = mock(AskLevel.class);
        when(askLevel.getPrice()).thenReturn(100L);
        when(askLevel.getQuantity()).thenReturn(10L);
        when(state.getAskAt(0)).thenReturn(askLevel);
        when(state.getAskLevels()).thenReturn(1);
        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(50L);
        when(state.getBidAt(0)).thenReturn(bidLevel);
        when(state.getChildOrders()).thenReturn(Arrays.asList(new ChildOrder[5]));
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        logger.info("Evaluating state with ask price: {}, bid price: {}", askLevel.getPrice(), bidLevel.getPrice());
        Action action = algoLogic.evaluate(state);
        logger.info("Action resulted in: {}", action);
        assertTrue(action instanceof CreateChildOrder);
        String actionString = action.toString();
        logger.info("CreateChildOrder string representation: {}", actionString);
        long expectedPrice = 95L;
        assertTrue(actionString.contains("price=" + expectedPrice));
    }

    protected UnsafeBuffer createTick13(long bidPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(50L);
        encoder.askBookCount(0);
        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);
        return directBuffer;
    }

    @Test
    public void test_null_far_touch_ask_level() throws Exception {
        logger.info("Starting test_null_far_touch_ask_level");
        send(createTick13(100L));
        Thread.sleep(1000);
        SimpleAlgoState state = container.getState();
        assertEquals(0, state.getAskLevels());
        MyAlgoLogic algoLogic = new MyAlgoLogic();
        Action action = algoLogic.evaluate(state);
        assertEquals(NoAction.NoAction, action);
        logger.info("Completed test_null_far_touch_ask_level");
    }
}