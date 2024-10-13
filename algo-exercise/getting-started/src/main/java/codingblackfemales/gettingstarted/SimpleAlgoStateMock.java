package codingblackfemales.gettingstarted;

import codingblackfemales.sotw.SimpleAlgoState;
import codingblackfemales.sotw.marketdata.AskLevel;
import codingblackfemales.sotw.marketdata.BidLevel;
import codingblackfemales.sotw.ChildOrder;
import codingblackfemales.sotw.OrderState;
import java.util.List;

public class SimpleAlgoStateMock implements SimpleAlgoState {
    private final List<BidLevel> bidLevels;
    private final List<AskLevel> askLevels;
    private final List<ChildOrder> childOrders;

    public SimpleAlgoStateMock(List<BidLevel> bidLevels, List<AskLevel> askLevels, List<ChildOrder> childOrders) {
        this.bidLevels = bidLevels;
        this.askLevels = askLevels;
        this.childOrders = childOrders;
    }

    @Override
    public String getSymbol() {
        return "TEST";
    }

    @Override
    public int getBidLevels() {
        return bidLevels.size();
    }

    @Override
    public int getAskLevels() {
        return askLevels.size();
    }

    @Override
    public BidLevel getBidAt(int index) {
        return bidLevels.get(index);
    }

    @Override
    public AskLevel getAskAt(int index) {
        return askLevels.get(index);
    }

    @Override
    public List<ChildOrder> getChildOrders() {
        return childOrders;
    }

    @Override
    public List<ChildOrder> getActiveChildOrders() {
        return childOrders.stream().filter(order -> order.getState() == OrderState.ACKED).toList();
    }

    @Override
    public long getInstrumentId() {
        return 1;
    }
}
