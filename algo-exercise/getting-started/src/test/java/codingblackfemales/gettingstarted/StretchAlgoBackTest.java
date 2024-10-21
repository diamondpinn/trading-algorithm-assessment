package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.service.OrderService;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoStateImpl;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StretchAlgoBackTest extends AbstractAlgoBackTest {
    private static final Logger logger = LoggerFactory.getLogger(StretchAlgoBackTest.class);
    private StretchAlgoLogic algoLogic;

    @Override
    public AlgoLogic createAlgoLogic() {
        algoLogic = new StretchAlgoLogic();
        return algoLogic;
    }

    protected UnsafeBuffer createTick1() {
        final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        final BookUpdateEncoder encoder = new BookUpdateEncoder();

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

        encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

        encoder.venue(Venue.XLON);
        encoder.instrumentId(123L);
        encoder.source(Source.STREAM);

        encoder.bidBookCount(10)
                .next().price(95L).size(100L)
                .next().price(94L).size(150L)
                .next().price(93L).size(200L)
                .next().price(92L).size(250L)
                .next().price(91L).size(300L)
                .next().price(90L).size(350L)
                .next().price(89L).size(400L)
                .next().price(88L).size(450L)
                .next().price(87L).size(500L)
                .next().price(86L).size(550L);

        encoder.askBookCount(10)
                .next().price(100L).size(500L)
                .next().price(101L).size(550L)
                .next().price(102L).size(600L)
                .next().price(103L).size(650L)
                .next().price(104L).size(700L)
                .next().price(105L).size(750L)
                .next().price(106L).size(800L)
                .next().price(107L).size(850L)
                .next().price(110L).size(900L)
                .next().price(150L).size(950L);

        encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

        return directBuffer;
    }

    private double calculateIcebergTotalCost() {
        double totalCost = 0.0;

        for (ChildOrder order : algoLogic.buyOrders) {
            if (isIcebergOrder(order)) {
                totalCost += order.getQuantity() * order.getPrice();
            } else {
                totalCost += order.getQuantity() * order.getPrice();
            }
        }

        return totalCost;
    }

    private boolean isIcebergOrder(ChildOrder order) {
        return order.getQuantity() < 100 && order.getPrice() == 100; // Example condition
    }

    @Test
    public void testProfitCalculationInLiquidMarketScenario() throws Exception {
        logger.info("Starting testProfitCalculationInLiquidMarketScenario");

        send(createTick1());
        Thread.sleep(1000);
        algoLogic.buyOrders.forEach(
                order -> logger.info("Buy Order - Quantity: " + order.getQuantity() + ", Price: " + order.getPrice()));

        send(createTick1());
        Thread.sleep(1000);

        // Now simulating a sell action with a selling price at $150.00 (in ticks)
        long quantityToSell = 200;
        double sellPrice = 150.00;

        double expectedTotalCost = calculateIcebergTotalCost();

        // Calculate the expected profit based on the calculated total cost
        double expectedProfit = (sellPrice * quantityToSell) - expectedTotalCost;

        double calculatedProfit = algoLogic.calculateProfit(quantityToSell, sellPrice);

        logger.info("Expected Total Cost: " + expectedTotalCost);
        logger.info("Expected Profit: " + expectedProfit);
        logger.info("Calculated Profit: " + calculatedProfit);

        // Verify that the calculated profit matches the expected profit
        assertEquals(expectedProfit, calculatedProfit, 0.01); // using a delta for floating point comparison

        logger.info("Completed testProfitCalculationInLiquidMarketScenario");
    }

    @Test
    public void testInitialOrdersAndIcebergOrders() throws Exception {
        logger.info("Starting testInitialOrdersAndIcebergOrders");

        // Send initial tick to create 5 buy orders
        send(createTick1());
        Thread.sleep(1000);

        assertEquals("Expected initial buy orders count to be 5", 5, algoLogic.buyOrders.size());

        // Attempt to place an iceberg order
        long quantityToBuy = 20;
        double buyPrice = 95.0;
        algoLogic.placeIcebergOrder(Side.BUY, quantityToBuy, buyPrice);

        Thread.sleep(1000);

        int totalOrders = algoLogic.buyOrders.size();
        logger.info("Total Buy Orders Count after placing iceberg order: " + totalOrders);

        // Check if the total orders count is valid
        assertTrue("Expected buy orders count to be less than or equal to maxOrders",
                totalOrders <= algoLogic.maxOrders);

        algoLogic.buyOrders.forEach(
                order -> logger.info("Buy Order - Quantity: " + order.getQuantity() + ", Price: " + order.getPrice()));

        logger.info("Completed testInitialOrdersAndIcebergOrders");
    }

    @Test
    public void testSMAIsCalculatedCorrectly() throws Exception {
        logger.info("Starting testSMAIsCalculatedCorrectly");

        UnsafeBuffer tick = createTick1();
        send(tick);

        // Create an instance of RunTrigger
        RunTrigger runTrigger = new RunTrigger();
        MarketDataService marketDataService = new MarketDataService(runTrigger);
        OrderService orderService = new OrderService(runTrigger);
        // Create the state directly using the real services
        SimpleAlgoStateImpl state = new SimpleAlgoStateImpl(marketDataService, orderService);

        Thread.sleep(1000);

        // Manually populate the recent prices to simulate the state
        algoLogic.recentPrices.clear(); // Clear any existing prices
        algoLogic.recentPrices.add(95.0);
        algoLogic.recentPrices.add(94.0);
        algoLogic.recentPrices.add(93.0);
        algoLogic.recentPrices.add(92.0);
        algoLogic.recentPrices.add(91.0);

        // Calculate SMA using the best bid price (latest price)
        double currentPrice = 95.0;
        double calculatedSMA = algoLogic.calculateSMA(state, currentPrice);

        double expectedSMA = (95.0 + 94.0 + 93.0 + 92.0 + 91.0) / 5.0;
        assertEquals("SMA should be calculated correctly", expectedSMA, calculatedSMA, 0.01);

        logger.info("Completed testSMAIsCalculatedCorrectly");
    }

}