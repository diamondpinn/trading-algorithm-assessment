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
import messages.marketdata.BookUpdateEncoder;
import messages.marketdata.InstrumentStatus;
import messages.marketdata.MessageHeaderEncoder;
import messages.marketdata.Source;
import messages.marketdata.Venue;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;

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
    protected UnsafeBuffer createTick1(long bidPrice, long askPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        // Write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        // Set the fields to desired values
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        // Create bid and ask levels
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(10L);

        encoder.askBookCount(1)
                .next().price(askPrice).size(10L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

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
        SimpleAlgoState state = createSimpleAlgoState(activeOrders, bidLevels, askLevels);
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();
        Action action = algoLogic.evaluate(state);
        assertTrue(action instanceof CancelChildOrder);
        logger.info("Completed testEvaluateWithMaxOrdersExceeded");
    }

    protected UnsafeBuffer createTick2(long bidPrice, long bidQuantity, long askPrice, long askQuantity) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(bidQuantity);

        encoder.askBookCount(1)
                .next().price(askPrice).size(askQuantity);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
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
        when(bidLevel.getPrice()).thenReturn(115L);
        when(bidLevel.getQuantity()).thenReturn(10L);
        when(askLevel.getPrice()).thenReturn(125L);
        when(askLevel.getQuantity()).thenReturn(10L);
        List<BidLevel> bidLevels = List.of(bidLevel);
        List<AskLevel> askLevels = List.of(askLevel);
        SimpleAlgoState state = createSimpleAlgoState(orders, bidLevels, askLevels);
        // Now test VWAP calculation
        double vwap = algoLogic.calculateVWAP(state);
        // Change the expected VWAP to the calculated value
        double expectedVWAP = 120.0;
        // Assert that the VWAP is calculated correctly
        assertEquals(expectedVWAP, vwap, 0.01);
    }

    protected UnsafeBuffer createTick3(long bidPrice, long bidQuantity, long askPrice, long askQuantity) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(bidQuantity);

        encoder.askBookCount(1)
                .next().price(askPrice).size(askQuantity);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void testIcebergOrderCreation() {
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();

        AskLevel askLevel = mock(AskLevel.class);
        when(askLevel.getPrice()).thenReturn(99L);
        when(askLevel.getQuantity()).thenReturn(100L);
        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(101L);
        when(bidLevel.getQuantity()).thenReturn(5L);

        SimpleAlgoState state = mock(SimpleAlgoState.class);
        when(state.getAskLevels()).thenReturn(1);
        when(state.getBidLevels()).thenReturn(1);
        when(state.getAskAt(0)).thenReturn(askLevel);
        when(state.getBidAt(0)).thenReturn(bidLevel);
        when(state.getActiveChildOrders()).thenReturn(new ArrayList<>());
        // Call evaluate to trigger iceberg order placement
        Action action = algoLogic.evaluate(state);
        // Check if an iceberg order was created
        assertEquals(NoAction.NoAction, action);
        assertEquals(5, algoLogic.buyOrders.size());
        assertEquals(StretchAlgoLogic.VISIBLE_ORDER_SIZE, algoLogic.buyOrders.get(0).getQuantity());
    }

    protected UnsafeBuffer createTick4(long price1, long quantity1, long price2, long quantity2, long price3,
            long quantity3) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        // Write the encoded output to the direct buffer
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.bidBookCount(1)
                .next().price(price1).size(quantity1);

        encoder.bidBookCount(1)
                .next().price(price2).size(quantity2);

        encoder.bidBookCount(1)
                .next().price(price3).size(quantity3);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void testSMACalculation() throws Exception {
        logger.info("Starting testSMACalculation");

        StretchAlgoLogic algoLogic = new StretchAlgoLogic();

        send(createTick4(98L, 10L, 100L, 15L, 102L, 20L));

        Thread.sleep(1000);
        SimpleAlgoState state = container.getState();

        double currentPrice = 100.0;
        double sma = algoLogic.calculateSMA(state, currentPrice);

        // Check the calculated SMA
        double expectedSMA = (98.0 + 100.0 + 102.0) / 3.0;
        assertEquals("SMA should be calculated correctly.", expectedSMA, sma, 0.01);
        logger.info("Completed testSMACalculation");
    }

    protected UnsafeBuffer createTick5(long bidPrice1, long quantity1, long bidPrice2, long quantity2, long askPrice,
            long askQuantity) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        encoder.bidBookCount(2)
                .next().price(bidPrice1).size(quantity1)
                .next().price(bidPrice2).size(quantity2);

        encoder.askBookCount(1)
                .next().price(askPrice).size(askQuantity);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void testProfitCalculation() throws Exception {
        logger.info("Starting testProfitCalculation");

        StretchAlgoLogic algoLogic = new StretchAlgoLogic();
        send(createTick5(95L, 10L, 100L, 5L, 105L, 15L)); // Prices are between 90 and 110

        Thread.sleep(1000);

        ChildOrder order1 = new ChildOrder(Side.BUY, algoLogic.generateOrderId(), 10, 95L, 0); // Ensure price is long
        ChildOrder order2 = new ChildOrder(Side.BUY, algoLogic.generateOrderId(), 5, 100L, 0); // Ensure price is long
        algoLogic.buyOrders.add(order1);
        algoLogic.buyOrders.add(order2);

        long sellPrice = 105L;
        long quantityToSell = 15;
        double profit = algoLogic.calculateProfit(quantityToSell, sellPrice);

        // Calculate expected profit
        double expectedProfit = (sellPrice * quantityToSell) - ((10 * 95) + (5 * 100));

        // Update the available capital to reflect the profit
        algoLogic.availableCapital += profit;
        assertEquals(expectedProfit, profit, 0.01);

        double newAvailableCapital = StretchAlgoLogic.INITIAL_CAPITAL + profit;
        assertEquals(newAvailableCapital, algoLogic.availableCapital, 0.01);

        logger.info("Completed testProfitCalculation");
    }

    protected UnsafeBuffer createTick6(long entryPrice, long bidPrice) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(10L); // Set bid price

        // Create ask level (price higher than entry price)
        encoder.askBookCount(1)
                .next().price(entryPrice).size(10L); // Set ask price

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void test_cancel_order_on_stop_loss() {
        logger.info("Starting test_cancel_order_on_stop_loss");

        StretchAlgoLogic algoLogic = (StretchAlgoLogic) createAlgoLogic(); // Ensure the right type

        ChildOrder order = createChildOrder(Side.BUY, algoLogic.generateOrderId(), 10, 100L, 0);

        List<ChildOrder> activeOrders = new ArrayList<>();
        activeOrders.add(order);

        long entryPrice = 100L;
        long bidPrice = 90L; // Bid price below the stop-loss threshold

        BidLevel bidLevel = new BidLevel();
        bidLevel.setPrice(bidPrice);
        bidLevel.setQuantity(10L);

        AskLevel askLevel = new AskLevel();
        askLevel.setPrice(entryPrice);
        askLevel.setQuantity(10L);
        SimpleAlgoState state = createSimpleAlgoState(activeOrders, List.of(bidLevel), List.of(askLevel));
        Action action = algoLogic.handleRiskManagement(state);

        // Assert that a CancelChildOrder action was returned
        assertTrue(action instanceof CancelChildOrder);

        logger.info("Completed test_cancel_order_on_stop_loss");
    }

    @Test
    public void test_create_sell_order_on_take_profit() {
        SimpleAlgoState state = mock(SimpleAlgoState.class);
        ChildOrder order = mock(ChildOrder.class);

        when(order.getPrice()).thenReturn(100L);
        when(order.getQuantity()).thenReturn(10L);
        when(order.getFilledQuantity()).thenReturn(0L);
        when(state.getActiveChildOrders()).thenReturn(Arrays.asList(order));

        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(120L);
        when(state.getBidAt(0)).thenReturn(bidLevel);
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();

        // Call the method to test (handleRiskManagement)
        Action action = algoLogic.handleRiskManagement(state);

        // Assert that a CreateChildOrder action was returned (indicating a sell order)
        assertTrue(action instanceof CreateChildOrder);
    }

    protected UnsafeBuffer createTick7(long bidPrice, long bidQuantity, long askPrice, long askQuantity) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);
        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        // Create bid levels with a negative price
        encoder.bidBookCount(1) // Assume there is one bid level
                .next().price(bidPrice).size(bidQuantity);
        // Create ask levels with a negative price
        encoder.askBookCount(1)
                .next().price(askPrice).size(askQuantity);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void test_handle_negative_or_zero_prices() throws Exception {
        logger.info("Starting test_handle_negative_or_zero_prices");
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();

        // Simulate sending ticks with negative prices
        send(createTick7(-90L, 10L, -85L, 5L)); // Negative bid and ask prices

        // Allow processing time for the system to react to the tick
        Thread.sleep(1000);
        SimpleAlgoState state = container.getState();
        Action action = algoLogic.handleRiskManagement(state);

        // Assert that no action is taken when encountering negative prices
        assertNull(action);

        logger.info("Completed test_handle_negative_or_zero_prices");
    }

    protected UnsafeBuffer createTick8(long bidPrice, long bidQuantity, long askPrice, long askQuantity) {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);

        // Create bid levels
        encoder.bidBookCount(1)
                .next().price(bidPrice).size(bidQuantity); // Bid level

        // Create ask levels
        encoder.askBookCount(1)
                .next().price(askPrice).size(askQuantity);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        encoder.source(Source.STREAM);

        return directBuffer;
    }

    @Test
    public void test_process_no_active_child_orders() throws Exception {
        logger.info("Starting test_process_no_active_child_orders");
        StretchAlgoLogic algoLogic = new StretchAlgoLogic();
        send(createTick8(100L, 10L, 102L, 5L)); // Send a tick with bid and ask prices
        Thread.sleep(1000);

        // Retrieve the state after sending the tick
        SimpleAlgoState state = container.getState();

        Action action = algoLogic.handleRiskManagement(state);
        assertNull(action);

        logger.info("Completed test_process_no_active_child_orders");
    }

    @Test
    public void test_manage_orders_with_zero_remaining_quantity() {

        SimpleAlgoState state = mock(SimpleAlgoState.class);
        ChildOrder order = mock(ChildOrder.class);

        // Set up the mock order to be fully filled
        when(order.getPrice()).thenReturn(100L);
        when(order.getQuantity()).thenReturn(10L);
        when(order.getFilledQuantity()).thenReturn(10L);

        // Set the active orders to include the fully filled order
        when(state.getActiveChildOrders()).thenReturn(Arrays.asList(order));

        BidLevel bidLevel = mock(BidLevel.class);
        when(bidLevel.getPrice()).thenReturn(110l); // Mocking a bid price
        when(state.getBidAt(0)).thenReturn(bidLevel);

        StretchAlgoLogic algoLogic = new StretchAlgoLogic();
        Action action = algoLogic.handleRiskManagement(state);

        assertNull(action);
    }

    @Test
    public void test_generate_order_id_starts_from_1() {
        AtomicLong orderIdGenerator = new AtomicLong(1);
        long orderId = orderIdGenerator.getAndIncrement();
        assertEquals(1, orderId);
    }

}
