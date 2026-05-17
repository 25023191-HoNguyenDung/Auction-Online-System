package integration;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidDao;
import com.auction.server.dao.jdbc.JdbcAuctionDao;
import com.auction.server.dao.jdbc.JdbcBidDao;
import com.auction.server.dao.jdbc.JdbcUserDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
import com.auction.server.observer.AuctionEvent;
import com.auction.server.observer.AuctionEventPublisher;
import com.auction.server.observer.AuctionObserver;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuctionServiceImpl;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlaceBidIntegrationTest {

    private static AuctionService auctionService;
    private static AuctionDao auctionDao;
    private static BidDao bidDao;
    private static AuctionEventPublisher publisher;
    private static long testAuctionId;

    @BeforeAll
    static void setUp() throws AuctionConnectException, SQLException {
        auctionDao     = new JdbcAuctionDao();
        bidDao         = new JdbcBidDao();
        auctionService = new AuctionServiceImpl(auctionDao, bidDao, new JdbcUserDao());
        publisher      = AuctionEventPublisher.getInstance();

        Auction auction = new Auction();
        auction.setItem_id(1L);
        auction.setSeller_id(2L);
        auction.setStarting_price(new BigDecimal("5000000"));
        auction.setCurrent_price(new BigDecimal("5000000"));
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setStart_time(LocalDateTime.now().minusMinutes(5));
        auction.setEnd_time(LocalDateTime.now().plusHours(2));
        auction.setWinner_bidder_id(0);

        testAuctionId = auctionDao.save(auction).getId();
    }

    @AfterAll
    static void tearDown() {
        auctionDao.deleteById(testAuctionId);
    }

    @Test
    @Order(1)
    void testPlaceBidSuccess() throws InvalidBidException, AuctionConnectException, AuctionMisMatchException, AuctionTimeException {
        BidTransaction bid = auctionService.placeBid(
                testAuctionId, 3L, new BigDecimal("6000000"));

        assertNotNull(bid);
        assertTrue(bid.getId() > 0, "Bid phải được lưu vào DB với ID");
        assertEquals(0, new BigDecimal("6000000").compareTo(bid.getBidAmount()));
        assertEquals(3L, bid.getBidderId());


        Auction updated = auctionDao.findById(testAuctionId).orElseThrow();
        assertEquals(0, new BigDecimal("6000000").compareTo(updated.getCurrent_price()));
        assertEquals("3", updated.getWinner_bidder_id());
    }

    @Test
    @Order(2)
    void testPlaceBidTooLowThrowsException() {

        assertThrows(RuntimeException.class, () ->
                auctionService.placeBid(testAuctionId, 4L, new BigDecimal("5000000")));
    }

    @Test
    @Order(3)
    void testPlaceBidHigherSuccess() throws InvalidBidException, AuctionConnectException, AuctionMisMatchException, AuctionTimeException {
        BidTransaction bid = auctionService.placeBid(testAuctionId, 4L, new BigDecimal("7000000"));

        assertNotNull(bid);
        assertEquals(4L, bid.getBidderId());

        Auction updated = auctionDao.findById(testAuctionId).orElseThrow();
        assertEquals(0, new BigDecimal("7000000").compareTo(updated.getCurrent_price()));
        assertEquals("4", updated.getWinner_bidder_id());
    }

    @Test
    @Order(4)
    void testBidHistorySavedToDb() {
        List<BidTransaction> history = auctionService.getBidHistory(testAuctionId);
        assertTrue(history.size() >= 2, "Phải có ít nhất 2 bid trong lịch sử");
        for (int i = 1; i < history.size(); i++) {
            assertFalse(history.get(i).getTimeBidding().isBefore(history.get(i - 1).getTimeBidding()),
                    "Bid sau phải có thời gian >= bid trước");
        }
    }

    @Test
    @Order(5)
    void testObserverReceivesBidEvent() throws InvalidBidException, AuctionConnectException, AuctionMisMatchException, AuctionTimeException {
        List<AuctionEvent> events = new ArrayList<>();
        AuctionObserver observer = events::add;
        publisher.subscribe(testAuctionId, observer);

        auctionService.placeBid(testAuctionId, 3L, new BigDecimal("8000000"));

        assertFalse(events.isEmpty(), "Observer phải nhận được BID_PLACED event");
        assertEquals(AuctionEvent.Type.BID_PLACED, events.get(0).getType());
        assertEquals(testAuctionId, events.get(0).getAuctionId());
        assertEquals(0, new BigDecimal("8000000").compareTo(events.get(0).getCurrentPrice()));

        publisher.unsubscribe(testAuctionId, observer);
    }

    @Test
    @Order(6)
    void testCloseAuction() throws AuctionConnectException, AuctionTimeException {
        auctionService.cancelAuction(testAuctionId);

        Auction closed = auctionDao.findById(testAuctionId).orElseThrow();
        assertEquals(AuctionStatus.FINISHED, closed.getStatus(),
                "Phiên phải chuyển sang FINISHED sau closeAuction");
        assertNotNull(closed.getWinner_bidder_id(), "Phải có winner sau khi đóng phiên");
    }

    @Test
    @Order(7)
    void testPlaceBidOnClosedAuctionThrowsException() {
        // Phiên đã FINISHED — không thể đặt giá nữa
        assertThrows(RuntimeException.class, () ->
                auctionService.placeBid(testAuctionId, 4L, new BigDecimal("9000000")));
    }
}
