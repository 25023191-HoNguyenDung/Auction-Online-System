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
// trung tâm phát thông báo
class AuctionEventPublisherTest {

    private AuctionEventPublisher publisher; // bp gửi thông báo

    @BeforeEach
    void setUp() {
        publisher = AuctionEventPublisher.getInstance();
    }


    @Test // khi publisher gửi event thì observer đó phải nhận được event
    void testPublishToSubscriber() {
        long auctionId = 999L; // tạo 1 phiên đgia
        List<AuctionEvent> received = new ArrayList<>(); // ds event observer nhận đc

        AuctionObserver observer = received::add; // khi observer nhận đc event thì thêm vào ds received
        publisher.subscribe(auctionId, observer); // oberserver bđ theo dõi phiên 99

        publisher.publish(AuctionEvent.bidPlaced(auctionId, new BigDecimal("1000000"), 3L)); // phát event

        assertEquals(1, received.size()); // ktra có trong received chưa
        assertEquals(AuctionEvent.Type.BID_PLACED, received.get(0).getType()); // ktra status
        assertEquals(auctionId, received.get(0).getAuctionId()); // ktra id

        publisher.unsubscribe(auctionId, observer); // hủy theo dõi
    }

    @Test // khi observer hủy đki thì ko nhận thêm event nào nữa
    void testUnsubscribeStopsReceiving() {
        long auctionId = 998L; // tạo auctionId
        List<AuctionEvent> received = new ArrayList<>(); //ds event
        AuctionObserver observer = received::add; // event obsever nhận đc chuyển vào received

        publisher.subscribe(auctionId, observer); // đki
        publisher.unsubscribe(auctionId, observer); // hủy đki

        publisher.publish(AuctionEvent.bidPlaced(auctionId, new BigDecimal("1000000"), 3L)); // phát event mới

        assertEquals(0, received.size(), "Sau unsubscribe không được nhận event"); // ktra xem có nhận đc ko
    }

    @Test // khi nhiều observer cùng đki thì phải cùng đc phát event
    void testMultipleObserversReceiveEvent() {
        long auctionId = 997L; // tạo auctionId
        // tạo ds nhận event
        List<AuctionEvent> received1 = new ArrayList<>();
        List<AuctionEvent> received2 = new ArrayList<>();
        List<AuctionEvent> received3 = new ArrayList<>();
        // ds event observer nhận được
        AuctionObserver o1 = received1::add;
        AuctionObserver o2 = received2::add;
        AuctionObserver o3 = received3::add;
        // đki
        publisher.subscribe(auctionId, o1);
        publisher.subscribe(auctionId, o2);
        publisher.subscribe(auctionId, o3);
        // phát event
        publisher.publish(AuctionEvent.bidPlaced(auctionId, new BigDecimal("5000000"), 4L));
        // ktra có trog received ko
        assertEquals(1, received1.size());
        assertEquals(1, received2.size());
        assertEquals(1, received3.size());
        // hủy đki
        publisher.unsubscribe(auctionId, o1);
        publisher.unsubscribe(auctionId, o2);
        publisher.unsubscribe(auctionId, o3);
    }

    @Test // test observer chỉ nhận event mà auction nó đki
    void testPublishToWrongAuctionIdNotReceived() {
        long auctionId = 996L; // tạo auctionId
        List<AuctionEvent> received = new ArrayList<>(); // ds các event
        AuctionObserver observer = received::add; // ds event observer nhận

        publisher.subscribe(auctionId, observer); // dki

        // tạo event
        publisher.publish(AuctionEvent.bidPlaced(auctionId + 1, new BigDecimal("1000000"), 3L));
        // ktra event nhận đc ko
        assertEquals(0, received.size(), "Không được nhận event của phiên khác");
        // hủy đki
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

}
