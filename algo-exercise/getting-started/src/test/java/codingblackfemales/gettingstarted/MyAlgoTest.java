package codingblackfemales.gettingstarted;

import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.OrderState;
import codingblackfemales.gettingstarted.MyAlgoLogic;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
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

}