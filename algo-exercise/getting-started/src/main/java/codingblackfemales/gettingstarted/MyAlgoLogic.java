package codingblackfemales.gettingstarted;

import codingblackfemales.action.Action;
import codingblackfemales.action.CancelChildOrder;
import codingblackfemales.action.CreateChildOrder;
import codingblackfemales.action.NoAction;
import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.util.Util;
import messages.order.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MyAlgoLogic implements AlgoLogic {

    private static final Logger logger = LoggerFactory.getLogger(MyAlgoLogic.class);
    protected static final int MAX_ORDER_COUNT = 8;
    private static final double MEAN_REVERSION_THRESHOLD = 0.05;
    private static final double TAKE_PROFIT_PERCENTAGE = 0.02;
    private static final double STOP_LOSS_PERCENTAGE = 0.02;

    public MyAlgoLogic() {

    }

    @Override
    public Action evaluate(SimpleAlgoState state) {
        // Log the current state of the order book
        String orderBookAsString = Util.orderBookToString(state);
        logger.info("[MYALGO] The state of the order book is:\n" + orderBookAsString);

        // Ensure there are ask levels available
        if (state.getAskLevels() == 0) {
            logger.info("[MYALGO] No ask levels available. Exiting evaluation.");
            return NoAction.NoAction; // Exit if no ask levels
        }

        // Retrieve total child orders
        int totalOrderCount = state.getChildOrders().size();

        // Handle the far touch logic first
        if (totalOrderCount < MAX_ORDER_COUNT) {
            AskLevel farTouch = state.getAskAt(0); // Get the far touch (best ask level)
            if (farTouch != null) {
                long quantity = farTouch.getQuantity(); // Use the quantity at the best ask level
                long price = farTouch.getPrice(); // Use the price at the best ask level

                // Mean reversion check
                long bidPrice = state.getBidAt(0).getPrice();
                long meanPrice = (price + bidPrice) / 2; // Calculate mean price
                if (Math.abs(price - meanPrice) / (double) meanPrice > MEAN_REVERSION_THRESHOLD) {
                    logger.info("[MYALGO] Current ask price deviated from mean: " +
                            (price - meanPrice) + ", adjusting price for order creation.");
                    price *= 0.95; // Adjusting to 95% of the price for future orders
                }

                logger.info("[MYALGO] Have: " + totalOrderCount + " children, want " + MAX_ORDER_COUNT +
                        ", sniping far touch of book with: " + quantity + " @ " + price);
                return new CreateChildOrder(Side.BUY, quantity, price); // Create a new child order
            }
        }

        // After attempting to create new orders, handle existing orders
        List<ChildOrder> activeOrders = state.getActiveChildOrders();

        // Check if we need to cancel an active order
        if (!activeOrders.isEmpty() && totalOrderCount >= MAX_ORDER_COUNT) {
            ChildOrder oldestOrder = activeOrders.get(0);
            logger.info("[MYALGO] Cancelling oldest active order: " + oldestOrder);
            return new CancelChildOrder(oldestOrder);
        }

        // Logic to match orders
        if (!activeOrders.isEmpty()) {
            for (ChildOrder order : activeOrders) {
                long orderPrice = order.getPrice();
                if (state.getBidAt(0) != null && state.getBidAt(0).getPrice() >= orderPrice) {
                    logger.info("[MYALGO] Order at price: " + orderPrice + " matched by market bid.");
                    // Order filled, execute logic for take profit and stop loss
                    return handleOrderExecution(order, state);
                }
            }
        }

        return NoAction.NoAction;
    }

    protected Action handleOrderExecution(ChildOrder order, SimpleAlgoState state) {
        long filledQuantity = order.getFilledQuantity(); // Get the filled quantity
        long filledPrice = order.getPrice(); // Get the filled price

        order.addFill(order.getQuantity(), filledPrice); // Use this to add fills
        updateChildOrderFill(order, filledPrice, order.getFilledQuantity());

        // Use this to calculate take profit and stop loss prices
        long takeProfitPrice = filledPrice + (long) (filledPrice * TAKE_PROFIT_PERCENTAGE);
        long stopLossPrice = filledPrice - (long) (filledPrice * STOP_LOSS_PERCENTAGE);

        logger.info("[MYALGO] Order filled at price: " + filledPrice +
                ". Setting take profit at: " + takeProfitPrice +
                " and stop loss at: " + stopLossPrice);

        // Check filled quantity to implement take profit and stop loss
        if (filledQuantity > 0) {
            // Only check the market price if the order has been filled
            if (state.getBidAt(0) != null) {
                long currentBidPrice = state.getBidAt(0).getPrice();
                if (currentBidPrice <= stopLossPrice) {
                    // Stop loss triggered, cancel the order
                    logger.info("[MYALGO] Triggering stop loss. Current bid price: " + currentBidPrice +
                            ", stop loss price: " + stopLossPrice);
                    return new CancelChildOrder(order); // Return action to cancel the order
                }
                if (currentBidPrice >= takeProfitPrice) {
                    logger.info("[MYALGO] Triggering take profit. Current bid price: " + currentBidPrice +
                            ", take profit price: " + takeProfitPrice);
                    return new CancelChildOrder(order);
                }
            }
        }

        return NoAction.NoAction;
    }

    // Method to update filled quantities and order state in the ChildOrder
    public void updateChildOrderFill(ChildOrder order, long filledQuantity, long filledPrice) {
        order.addFill(filledQuantity, filledPrice); // Track the fill
        if (order.getFilledQuantity() >= order.getQuantity()) {
            order.setState(OrderState.FILLED); // Update state to FILLED if fully filled
        }
    }
}