package codingblackfemales.gettingstarted;

import codingblackfemales.algo.AlgoLogic;
import codingblackfemales.container.Actioner;
import codingblackfemales.container.AlgoContainer;
import codingblackfemales.container.RunTrigger;
import codingblackfemales.orderbook.OrderBook;
import codingblackfemales.orderbook.channel.MarketDataChannel;
import codingblackfemales.orderbook.channel.OrderChannel;
import codingblackfemales.orderbook.consumer.OrderBookInboundOrderConsumer;
import codingblackfemales.sequencer.DefaultSequencer;
import codingblackfemales.sequencer.Sequencer;
import codingblackfemales.sequencer.consumer.LoggingConsumer;
import codingblackfemales.sequencer.marketdata.SequencerTestCase;
import codingblackfemales.sequencer.net.TestNetwork;
import codingblackfemales.service.MarketDataService;
import codingblackfemales.service.OrderService;
import messages.marketdata.*;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public abstract class AbstractAlgoBackTest extends SequencerTestCase {

        protected AlgoContainer container;

        @Override
        public Sequencer getSequencer() {
                final TestNetwork network = new TestNetwork();
                final Sequencer sequencer = new DefaultSequencer(network);

                final RunTrigger runTrigger = new RunTrigger();
                final Actioner actioner = new Actioner(sequencer);

                final MarketDataChannel marketDataChannel = new MarketDataChannel(sequencer);
                final OrderChannel orderChannel = new OrderChannel(sequencer);
                final OrderBook book = new OrderBook(marketDataChannel, orderChannel);

                final OrderBookInboundOrderConsumer orderConsumer = new OrderBookInboundOrderConsumer(book);

                container = new AlgoContainer(new MarketDataService(runTrigger), new OrderService(runTrigger),
                                runTrigger,
                                actioner);
                // set my algo logic
                container.setLogic(createAlgoLogic());

                network.addConsumer(new LoggingConsumer());
                network.addConsumer(book);
                network.addConsumer(container.getMarketDataService());
                network.addConsumer(container.getOrderService());
                network.addConsumer(orderConsumer);
                network.addConsumer(container);

                return sequencer;
        }

        public abstract AlgoLogic createAlgoLogic();

        protected UnsafeBuffer createTick(long bidPrice, long askPrice) {
                final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
                final BookUpdateEncoder encoder = new BookUpdateEncoder();

                final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

                // write the encoded output to the direct buffer
                encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

                // set the fields to desired values
                encoder.venue(Venue.XLON);
                encoder.instrumentId(123L);
                encoder.source(Source.STREAM);

                encoder.bidBookCount(5)
                                .next().price(bidPrice).size(100L) // Far touch bid
                                .next().price(bidPrice - 1).size(200L)
                                .next().price(bidPrice - 2).size(150L)
                                .next().price(bidPrice - 3).size(250L)
                                .next().price(bidPrice - 4).size(300L);

                encoder.askBookCount(5)
                                .next().price(askPrice).size(100L) // Far touch ask
                                .next().price(askPrice + 1).size(200L)
                                .next().price(askPrice + 2).size(150L)
                                .next().price(askPrice + 3).size(250L)
                                .next().price(askPrice + 4).size(300L);

                encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

                return directBuffer;
        }

        protected UnsafeBuffer createTick2() {

                final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
                final BookUpdateEncoder encoder = new BookUpdateEncoder();

                final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

                // write the encoded output to the direct buffer
                encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

                // set the fields to desired values
                encoder.venue(Venue.XLON);
                encoder.instrumentId(123L);
                encoder.source(Source.STREAM);

                encoder.bidBookCount(3)
                                .next().price(95L).size(100L)
                                .next().price(93L).size(200L)
                                .next().price(91L).size(300L);

                encoder.askBookCount(4)
                                .next().price(98L).size(501L)
                                .next().price(101L).size(200L)
                                .next().price(110L).size(5000L)
                                .next().price(119L).size(5600L);

                encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

                return directBuffer;

        }

        protected UnsafeBuffer createTick3() {

                final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
                final BookUpdateEncoder encoder = new BookUpdateEncoder();

                final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                final UnsafeBuffer directBuffer = new UnsafeBuffer(byteBuffer);

                // write the encoded output to the direct buffer
                encoder.wrapAndApplyHeader(directBuffer, 0, headerEncoder);

                // set the fields to desired values
                encoder.venue(Venue.XLON);
                encoder.instrumentId(123L);
                encoder.source(Source.STREAM);

                encoder.bidBookCount(3)
                                .next().price(95L).size(100L)
                                .next().price(93L).size(200L)
                                .next().price(91L).size(300L);

                encoder.askBookCount(4)
                                .next().price(99L).size(501L)
                                .next().price(104L).size(200L)
                                .next().price(113L).size(5000L)
                                .next().price(121L).size(5600L);

                encoder.instrumentStatus(InstrumentStatus.CONTINUOUS);

                return directBuffer;

        }

}