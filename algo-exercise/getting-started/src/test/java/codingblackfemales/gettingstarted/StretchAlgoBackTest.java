package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import messages.marketdata.InstrumentStatus;
import messages.marketdata.Source;
import messages.marketdata.Venue;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import messages.marketdata.BookUpdateEncoder;
import messages.marketdata.MessageHeaderEncoder;
import java.nio.ByteBuffer;
import static org.junit.Assert.assertEquals;

/**
 * This test plugs together all of the infrastructure, including the order book
 * (which you can trade against)
 * and the market data feed.
 *
 * If your algo adds orders to the book, they will reflect in your market data
 * coming back from the order book.
 *
 * If you cross the spread (i.e. you BUY an order with a price which is == or >
 * askPrice()) you will match,
 * and receive a fill back into your order from the order book (visible from the
 * algo in the childOrders of the state object).
 *
 * If you cancel the order your child order will show the order status as
 * cancelled in the childOrders of the state object.
 *
 */
public class StretchAlgoBackTest extends AbstractAlgoBackTest {

    @Override
    public AlgoLogic createAlgoLogic() {
        return new StretchAlgoLogic();
    }

    @Test
    public void testAlgoBackTestScenarios() throws Exception {
        // Sending all 10 unique ticks and checking conditions after each one
        // Scenario 1: Create a stable market
        send(createStableMarketScenario());
        validateScenario(1, 3, 0, 0); // 3 orders expected, 0 filled, 0 canceled

        // Scenario 2: Create a bullish market
        send(createBullishMarketScenario());
        validateScenario(2, 4, 225, 0); // 4 orders expected, 225 filled, 0 canceled

        // Scenario 3: Create a bearish market
        send(createBearishMarketScenario());
        validateScenario(3, 2, 100, 1); // 2 orders expected, 100 filled, 1 canceled

        // Scenario 4: Create a volatile market
        send(createVolatileMarketScenario());
        validateScenario(4, 5, 300, 2); // 5 orders expected, 300 filled, 2 canceled

        // Scenario 5: Create a flat market
        send(createFlatMarketScenario());
        validateScenario(5, 3, 0, 1); // 3 orders expected, 0 filled, 1 canceled

        // Scenario 6: Create a rapidly rising market
        send(createRapidlyRisingMarketScenario());
        validateScenario(6, 4, 400, 0); // 4 orders expected, 400 filled, 0 canceled

        // Scenario 7: Create a rapidly falling market
        send(createRapidlyFallingMarketScenario());
        validateScenario(7, 2, 50, 1); // 2 orders expected, 50 filled, 1 canceled

        // Scenario 8: Create a sideways market
        send(createSidewaysMarketScenario());
        validateScenario(8, 3, 150, 1); // 3 orders expected, 150 filled, 1 canceled

        // Scenario 9: Create a market with high liquidity
        send(createHighLiquidityMarketScenario());
        validateScenario(9, 5, 500, 0); // 5 orders expected, 500 filled, 0 canceled

        // Scenario 10: Create a market with low liquidity
        send(createLowLiquidityMarketScenario());
        validateScenario(10, 2, 30, 1); // 2 orders expected, 30 filled, 1 canceled
    }

    private void validateScenario(int scenarioNumber, int expectedOrders, long expectedFilledQuantity,
            long expectedCanceledOrders) {
        // Get the current state after sending the tick
        var state = container.getState();

        // Check the expected number of child orders
        assertEquals("Scenario " + scenarioNumber + ": Unexpected number of orders.", expectedOrders,
                state.getChildOrders().size());

        // Check the filled quantity
        long filledQuantity = state.getChildOrders().stream()
                .map(ChildOrder::getFilledQuantity)
                .reduce(Long::sum)
                .orElse(0L);
        assertEquals("Scenario " + scenarioNumber + ": Unexpected filled quantity.", expectedFilledQuantity,
                filledQuantity);

        // Check for canceled orders
        long canceledOrders = state.getChildOrders().stream()
                .filter(order -> order.getState() == OrderState.CANCELLED)
                .count();
        assertEquals("Scenario " + scenarioNumber + ": Unexpected number of canceled orders.",
                expectedCanceledOrders,
                canceledOrders);
    }

    // Sample implementations for the unique tick scenarios.
    protected UnsafeBuffer createStableMarketScenario() {
        return createTick(); // Reusing createTick() for a stable scenario
    }

    protected UnsafeBuffer createBullishMarketScenario() {
        return createTick2(); // Using a variation of createTick() for bullish scenario
    }

    protected UnsafeBuffer createBearishMarketScenario() {
        // Create a unique bearish market scenario tick
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(2)
                .next().price(90L).size(100L)
                .next().price(88L).size(200L);

        encoder.askBookCount(3)
                .next().price(92L).size(101L)
                .next().price(93L).size(200L)
                .next().price(95L).size(300L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        return directBuffer;
    }

    protected UnsafeBuffer createVolatileMarketScenario() {
        // Create a unique volatile market scenario tick
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(5)
                .next().price(98L).size(100L)
                .next().price(96L).size(200L)
                .next().price(95L).size(150L)
                .next().price(94L).size(250L)
                .next().price(93L).size(300L);

        encoder.askBookCount(5)
                .next().price(99L).size(500L)
                .next().price(100L).size(600L)
                .next().price(101L).size(700L)
                .next().price(102L).size(800L)
                .next().price(103L).size(900L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        return directBuffer;
    }

    protected UnsafeBuffer createFlatMarketScenario() {
        // Create a unique flat market scenario tick
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(3)
                .next().price(97L).size(150L)
                .next().price(96L).size(150L)
                .next().price(95L).size(150L);

        encoder.askBookCount(3)
                .next().price(97L).size(150L)
                .next().price(96L).size(150L)
                .next().price(95L).size(150L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        return directBuffer;
    }

    protected UnsafeBuffer createRapidlyRisingMarketScenario() {
        // Create a unique rapidly rising market scenario tick
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(4)
                .next().price(104L).size(400L)
                .next().price(105L).size(300L)
                .next().price(106L).size(500L)
                .next().price(107L).size(600L);

        encoder.askBookCount(4)
                .next().price(108L).size(700L)
                .next().price(109L).size(800L)
                .next().price(110L).size(900L)
                .next().price(111L).size(1000L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        return directBuffer;
    }

    protected UnsafeBuffer createRapidlyFallingMarketScenario() {
        // Create a unique rapidly falling market scenario tick
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(2)
                .next().price(82L).size(100L)
                .next().price(80L).size(50L);

        encoder.askBookCount(3)
                .next().price(84L).size(200L)
                .next().price(86L).size(300L)
                .next().price(88L).size(400L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        return directBuffer;
    }

    protected UnsafeBuffer createSidewaysMarketScenario() {
        // Create a unique sideways market scenario tick
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(4)
                .next().price(95L).size(150L)
                .next().price(96L).size(150L)
                .next().price(95L).size(150L)
                .next().price(94L).size(150L);

        encoder.askBookCount(4)
                .next().price(97L).size(150L)
                .next().price(96L).size(150L)
                .next().price(95L).size(150L)
                .next().price(94L).size(150L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        return directBuffer;
    }

    protected UnsafeBuffer createHighLiquidityMarketScenario() {
        // Create a unique high liquidity market scenario tick
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(5)
                .next().price(100L).size(1000L)
                .next().price(99L).size(2000L)
                .next().price(98L).size(1500L)
                .next().price(97L).size(1200L)
                .next().price(96L).size(900L);

        encoder.askBookCount(5)
                .next().price(101L).size(2000L)
                .next().price(102L).size(1800L)
                .next().price(103L).size(1600L)
                .next().price(104L).size(1500L)
                .next().price(105L).size(1400L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        return directBuffer;
    }

    protected UnsafeBuffer createLowLiquidityMarketScenario() {
        // Create a unique low liquidity market scenario tick
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);
        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(1)
                .next().price(100L).size(100L);

        encoder.askBookCount(1)
                .next().price(102L).size(50L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);
        return directBuffer;
    }
}
