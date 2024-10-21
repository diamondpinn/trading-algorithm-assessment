package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class StretchAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(StretchAlgoLogic.class);
    protected static final double INITIAL_CAPITAL = 100000.0;
    protected double availableCapital = INITIAL_CAPITAL;
    protected double stopLossPercentage = 0.03; // 3%
    private double takeProfitPercentage = 0.05; // 5%
    protected int maxOrders = 21; // Maximum allowed active orders
    protected double transactionCost = 0.001; // 0.1%

    // Iceberg Order Config
    protected static final long TOTAL_ORDER_SIZE = 100; // Total size of iceberg order
    protected static final long VISIBLE_ORDER_SIZE = 20; // Visible size of iceberg order

    // Track buy orders for profit calculation
    protected final List<ChildOrder> buyOrders = new ArrayList<>();

    // Unique order IDs
    protected static final AtomicLong orderIdGenerator = new AtomicLong(1);
    protected double totalProfit = 0.0; // Track total profit

    // SMA Config
    protected static final int SMA_PERIOD = 5; // Define a period for SMA calculation
    protected final List<Double> recentPrices = new ArrayList<>(); // Store recent prices for SMA calculation

    @Override
    public Action evaluate(SimpleAlgoState state) {
        // Log the current state of the order book
        logOrderBookState(state);

        // Ensure there are ask levels available
        if (state.getAskLevels() == 0) {
            logger.info("No ask levels available. Exiting evaluation.");
            return NoAction.NoAction; // Exit if no ask levels
        }

        // Retrieve the best ask and bid prices
        AskLevel bestAsk = state.getAskAt(0);
        BidLevel bestBid = state.getBidAt(0);

        // Calculate VWAP and SMA
        double vwap = calculateVWAP(state);
        double sma = calculateSMA(state, bestBid.getPrice()); // Include current price for SMA

        // Log the VWAP and SMA values
        logger.info("VWAP: " + vwap + ", SMA: " + sma + ", Best Ask Price: " + bestAsk.getPrice() + ", Best Bid Price: "
                + bestBid.getPrice());

        // Check if the order being placed is an iceberg order
        boolean isIcebergOrder = false;

        // Cancel orders if maxOrders limit is exceeded, unless it's an iceberg order
        if (!isIcebergOrder && state.getActiveChildOrders().size() >= maxOrders) {
            logger.info("Max orders limit exceeded. Cancelling excess orders.");
            ChildOrder orderToCancel = state.getActiveChildOrders().get(0); // Cancel the oldest order
            return new CancelChildOrder(orderToCancel);
        }

        // Trading logic based on VWAP and SMA
        if (bestAsk.getPrice() < vwap && availableCapital > 0) {
            long quantityToBuy = TOTAL_ORDER_SIZE; // Use TOTAL_ORDER_SIZE for the order quantity
            long cost = (long) (bestAsk.getPrice() * quantityToBuy * (1 + transactionCost));
            if (availableCapital >= cost) {
                logger.info("Placing buy order at best ask: " + Util.padLeft(String.valueOf(bestAsk.getPrice()), 10)
                        + " with quantity: " + quantityToBuy);
                availableCapital -= cost; // Update available capital

                // Place Iceberg Order
                isIcebergOrder = true; // Set to true as we are placing an iceberg order
                return placeIcebergOrder(Side.BUY, quantityToBuy, bestAsk.getPrice());
            } else {
                logger.info("Insufficient capital to place buy order. Available capital: " + availableCapital
                        + ", Cost: " + cost);
            }
        } else if (bestBid.getPrice() > vwap && !state.getActiveChildOrders().isEmpty()) {
            long quantityToSell = getQuantityToSell(state);
            if (quantityToSell > 0) {
                long sellPrice = (long) bestBid.getPrice();
                double profit = calculateProfit(quantityToSell, sellPrice);
                totalProfit += profit; // Update total profit
                logger.info("Selling quantity: " + quantityToSell + " at price: "
                        + Util.padLeft(String.valueOf(sellPrice), 10) + ". Profit: " + profit);
                logger.info("Total Profit So Far: " + Util.padLeft(String.valueOf(totalProfit), 10)); // Log total
                                                                                                      // profit
                return new CreateChildOrder(Side.SELL, quantityToSell, sellPrice);
            } else {
                logger.info("No quantity available to sell.");
            }
        }

        // Handle stop-loss and take-profit for active orders
        Action riskAction = handleRiskManagement(state);
        if (riskAction != null) {
            return riskAction; // Use action from risk management if not null
        }

        return NoAction.NoAction;
    }

    // Method to place Iceberg Order
    protected Action placeIcebergOrder(Side side, long totalQuantity, double price) {
        long remainingQuantity = totalQuantity;

        // Split order into visible and hidden parts
        while (remainingQuantity > 0) {
            long visibleQuantity = Math.min(VISIBLE_ORDER_SIZE, remainingQuantity);
            long orderPrice = (long) price; // Convert price to long before creating the ChildOrder
            ChildOrder icebergOrder = new ChildOrder(side, generateOrderId(), visibleQuantity, orderPrice, 0);
            buyOrders.add(icebergOrder); // Store buy order for profit tracking
            logger.info("Placing iceberg order: " + side + " " + visibleQuantity + " at " + orderPrice);
            remainingQuantity -= visibleQuantity;
        }
        // Log the iceberg order creation
        logger.info(
                "Iceberg order created: " + side + " with total quantity: " + totalQuantity + " at price: " + price);

        return NoAction.NoAction; // No action if nothing is placed
    }

    // Logs the current state of the order book
    protected void logOrderBookState(SimpleAlgoState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Order Book State:\n");

        // Log bid levels
        for (int i = 0; i < state.getBidLevels(); i++) {
            BidLevel bid = state.getBidAt(i);
            sb.append("BID: " + bid.getPrice() + " @ " + bid.getQuantity() + "\n");
        }

        // Log ask levels
        for (int i = 0; i < state.getAskLevels(); i++) {
            AskLevel ask = state.getAskAt(i);
            sb.append("ASK: " + ask.getPrice() + " @ " + ask.getQuantity() + "\n");
        }

        logger.info(sb.toString());
    }

    // Calculates the VWAP using the order book
    protected double calculateVWAP(SimpleAlgoState state) {
        double cumulativePriceVolume = 0.0;
        double cumulativeVolume = 0.0;

        // Calculate cumulative price-volume for bids
        for (int i = 0; i < state.getBidLevels(); i++) {
            BidLevel bid = state.getBidAt(i);
            cumulativePriceVolume += bid.getPrice() * bid.getQuantity();
            cumulativeVolume += bid.getQuantity();
        }

        // Calculate cumulative price-volume for asks
        for (int i = 0; i < state.getAskLevels(); i++) {
            AskLevel ask = state.getAskAt(i);
            cumulativePriceVolume += ask.getPrice() * ask.getQuantity();
            cumulativeVolume += ask.getQuantity();
        }

        return cumulativeVolume == 0 ? 0 : cumulativePriceVolume / cumulativeVolume;
    }

    // Calculates the SMA using recent prices
    protected double calculateSMA(SimpleAlgoState state, double currentPrice) {
        // Add the latest price to the recent prices list
        recentPrices.add(currentPrice);

        // Keep only the last SMA_PERIOD prices
        if (recentPrices.size() > SMA_PERIOD) {
            recentPrices.remove(0);
        }

        // Calculate the sum of the recent prices
        double sum = 0.0;
        for (double price : recentPrices) {
            sum += price;
        }

        return recentPrices.isEmpty() ? 0.0 : sum / recentPrices.size(); // Return average or 0
    }

    // Gets the total quantity available to sell from active orders
    protected long getQuantityToSell(SimpleAlgoState state) {
        long quantity = 0;
        for (ChildOrder order : state.getActiveChildOrders()) {
            quantity += order.getQuantity() - order.getFilledQuantity();
        }
        return quantity;
    }

    // Handles risk management actions for active orders
    public Action handleRiskManagement(SimpleAlgoState state) {
        for (ChildOrder order : state.getActiveChildOrders()) {
            double entryPrice = order.getPrice();
            double currentPrice = state.getBidAt(0).getPrice();
            double stopLossPrice = entryPrice * (1 - stopLossPercentage);
            double takeProfitPrice = entryPrice * (1 + takeProfitPercentage);

            logger.info("Risk Management - Order Entry Price: " + entryPrice + ", Current Price: " + currentPrice +
                    ", Stop-Loss Price: " + stopLossPrice + ", Take-Profit Price: " + takeProfitPrice);

            if (currentPrice <= stopLossPrice) {
                // Cancel the order to stop loss
                logger.info("Stop-loss triggered. Cancelling order at: " + currentPrice);
                return new CancelChildOrder(order); // Return the cancel action
            } else if (currentPrice >= takeProfitPrice) {
                // Sell to take profit
                logger.info("Take-profit triggered. Selling at: " + currentPrice);
                long quantityToSell = order.getQuantity() - order.getFilledQuantity(); // Get remaining quantity
                if (quantityToSell > 0) {
                    return new CreateChildOrder(Side.SELL, quantityToSell, (long) currentPrice); // Convert to long
                } else {
                    logger.info("No quantity left to sell.");
                }
            }
        }
        return null;
    }

    // Calculates profit for a trade
    protected double calculateProfit(long quantity, double sellPrice) {
        double totalCost = 0.0;
        long remainingQuantity = quantity;
        for (ChildOrder order : buyOrders) {
            if (remainingQuantity <= 0)
                break;
            long buyQuantity = order.getQuantity();
            if (remainingQuantity >= buyQuantity) {
                totalCost += buyQuantity * order.getPrice();
                remainingQuantity -= buyQuantity;
            } else {
                totalCost += remainingQuantity * order.getPrice();
                remainingQuantity = 0;
            }
        }
        double profit = (sellPrice * quantity) - totalCost; // Profit is selling price - total cost
        logger.info("Calculating profit - Quantity: " + quantity + ", Sell Price: " + sellPrice + ", Total Cost: "
                + totalCost + ", Profit: " + profit);
        return profit;
    }

    // Generates a unique order ID
    public long generateOrderId() {
        return orderIdGenerator.getAndIncrement();
    }

    // Use this as tatic inner class for PriceVolume
    public static class PriceVolume {
        private final double price;
        private final long volume;

        public PriceVolume(double price, long volume) {
            this.price = price;
            this.volume = volume;
        }

        public double getPrice() {
            return price;
        }

        public long getVolume() {
            return volume;
        }
    }

}
