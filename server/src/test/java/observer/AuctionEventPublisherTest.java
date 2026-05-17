package observer;

import com.auction.server.observer.AuctionEvent;
import com.auction.server.observer.AuctionEventPublisher;
import com.auction.server.observer.AuctionObserver;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionEventPublisherTest {

    private AuctionEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = AuctionEventPublisher.getInstance();
    }


    @Test
    void testPublishToSubscriber() {
        long auctionId = 999L;
        List<AuctionEvent> received = new ArrayList<>();

        AuctionObserver observer = received::add;
        publisher.subscribe(auctionId, observer);

        publisher.publish(AuctionEvent.bidPlaced(auctionId, new BigDecimal("1000000"), 3L));

        assertEquals(1, received.size());
        assertEquals(AuctionEvent.Type.BID_PLACED, received.get(0).getType());
        assertEquals(auctionId, received.get(0).getAuctionId());

        publisher.unsubscribe(auctionId, observer);
    }

    @Test
    void testUnsubscribeStopsReceiving() {
        long auctionId = 998L;
        List<AuctionEvent> received = new ArrayList<>();
        AuctionObserver observer = received::add;

        publisher.subscribe(auctionId, observer);
        publisher.unsubscribe(auctionId, observer);

        publisher.publish(AuctionEvent.bidPlaced(auctionId, new BigDecimal("1000000"), 3L));

        assertEquals(0, received.size(), "Sau unsubscribe không được nhận event");
    }

    @Test
    void testMultipleObserversReceiveEvent() {
        long auctionId = 997L;
        List<AuctionEvent> received1 = new ArrayList<>();
        List<AuctionEvent> received2 = new ArrayList<>();
        List<AuctionEvent> received3 = new ArrayList<>();

        AuctionObserver o1 = received1::add;
        AuctionObserver o2 = received2::add;
        AuctionObserver o3 = received3::add;

        publisher.subscribe(auctionId, o1);
        publisher.subscribe(auctionId, o2);
        publisher.subscribe(auctionId, o3);

        publisher.publish(AuctionEvent.bidPlaced(auctionId, new BigDecimal("5000000"), 4L));

        assertEquals(1, received1.size());
        assertEquals(1, received2.size());
        assertEquals(1, received3.size());

        publisher.unsubscribe(auctionId, o1);
        publisher.unsubscribe(auctionId, o2);
        publisher.unsubscribe(auctionId, o3);
    }

    @Test
    void testPublishToWrongAuctionIdNotReceived() {
        long auctionId = 996L;
        List<AuctionEvent> received = new ArrayList<>();
        AuctionObserver observer = received::add;

        publisher.subscribe(auctionId, observer);


        publisher.publish(AuctionEvent.bidPlaced(auctionId + 1, new BigDecimal("1000000"), 3L));

        assertEquals(0, received.size(), "Không được nhận event của phiên khác");

        publisher.unsubscribe(auctionId, observer);
    }

    @Test
    void testObserverExceptionDoesNotBlockOthers() {
        long auctionId = 995L;
        List<AuctionEvent> received = new ArrayList<>();


        AuctionObserver badObserver = event -> { throw new RuntimeException("Bad observer!"); };

        AuctionObserver goodObserver = received::add;

        publisher.subscribe(auctionId, badObserver);
        publisher.subscribe(auctionId, goodObserver);

        assertDoesNotThrow(() ->
                publisher.publish(AuctionEvent.bidPlaced(auctionId, new BigDecimal("1000000"), 3L)));

        assertEquals(1, received.size(), "Observer tốt vẫn phải nhận được event");

        publisher.unsubscribe(auctionId, badObserver);
        publisher.unsubscribe(auctionId, goodObserver);
    }

    @Test
    void testConcurrentSubscribeAndPublish() throws InterruptedException {
        long auctionId = 994L;
        int threadCount = 10;
        List<AuctionEvent> received = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    synchronized (received) {
                        publisher.subscribe(auctionId, received::add);
                    }
                } catch (InterruptedException ignored) {}
            });
        }
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    publisher.publish(AuctionEvent.bidPlaced(
                            auctionId, new BigDecimal("1000000"), 3L));
                } catch (InterruptedException ignored) {}
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        System.out.println("✓ Concurrent test passed. Events received: " + received.size());
    }

    @Test
    void testGetObserverCount() {
        long auctionId = 993L;
        assertEquals(0, publisher.getObserverCount(auctionId));

        AuctionObserver o1 = event -> {};
        AuctionObserver o2 = event -> {};
        publisher.subscribe(auctionId, o1);
        publisher.subscribe(auctionId, o2);

        assertEquals(2, publisher.getObserverCount(auctionId));

        publisher.unsubscribe(auctionId, o1);
        assertEquals(1, publisher.getObserverCount(auctionId));

        publisher.unsubscribe(auctionId, o2);
        assertEquals(0, publisher.getObserverCount(auctionId));
    }
}
