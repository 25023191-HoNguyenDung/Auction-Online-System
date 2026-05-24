package com.auction.server.integration;

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
import com.auction.server.service.AuctionServiceImpl;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // chạy theo thứ tự
// ktra chức năng đặt đgia
class PlaceBidIntegrationTest {

    private static AuctionServiceImpl auctionService; //Service chính cần test
    private static AuctionDao auctionDao;
    private static BidDao bidDao;
    private static AuctionEventPublisher publisher; // Quản lý observer/event
    private static long testAuctionId; // id của phiên test

    @BeforeAll
    static void setUp() throws AuctionConnectException, SQLException {
        auctionDao     = new JdbcAuctionDao();
        bidDao         = new JdbcBidDao();
        JdbcUserDao userDao = new JdbcUserDao();
        auctionService = new AuctionServiceImpl(auctionDao, bidDao, userDao);
        publisher      = AuctionEventPublisher.getInstance();

        // Đảm bảo các user test (3L, 4L) có đủ số dư tài khoản
        userDao.findById(3L).ifPresent(u -> {
            if (u instanceof com.auction.server.model.Bidder b) {
                b.setAccount_balance(new BigDecimal("20000000")); // 20 triệu
                userDao.update(b);
            }
        });
        userDao.findById(4L).ifPresent(u -> {
            if (u instanceof com.auction.server.model.Bidder b) {
                b.setAccount_balance(new BigDecimal("20000000")); // 20 triệu
                userDao.update(b);
            }
        });

        Auction auction = new Auction();// tạo phiên đgia mẫu
        // khởi tạo gtri bđầu
        auction.setItem_id(1L);
        auction.setSeller_id(2L);
        auction.setStarting_price(new BigDecimal("5000000"));
        auction.setCurrent_price(new BigDecimal("5000000"));
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setStart_time(LocalDateTime.now().minusMinutes(5));
        auction.setEnd_time(LocalDateTime.now().plusHours(2));
        auction.setWinner_bidder_id(0L);

        testAuctionId = auctionDao.save(auction).getId(); // lấy id
    }

    @AfterAll
    static void tearDown() {
        auctionDao.deleteById(testAuctionId);
    } // xóa phiên

    @Test // test đgia thành công
    @Order(1)
    void testPlaceBidSuccess() throws InvalidBidException, AuctionConnectException, AuctionMisMatchException, AuctionTimeException {
        BidTransaction bid = auctionService.placeBid(testAuctionId, 3L, new BigDecimal("6000000")); // đặt giá

        assertNotNull(bid); // ktra bid được tạo thành công
        assertTrue(bid.getId() > 0, "Bid phải được lưu vào DB với ID"); // ktra có trog db chưa
        assertEquals(0, new BigDecimal("6000000").compareTo(bid.getBidAmount())); // ktra giá đặt
        assertEquals(3L, bid.getBidderId()); // ktra người đặt giá


        Auction updated = auctionDao.findById(testAuctionId).orElseThrow(); // lấy auction trog db sau khi đặt xog
        assertEquals(0, new BigDecimal("6000000").compareTo(updated.getCurrent_price())); // current_price phải bằng giá vừa đặt
        assertEquals(3L, updated.getWinner_bidder_id()); // ktra người dẫn đầu
    }

    @Test // đặt giá thấp hơn htai
    @Order(2)
    void testPlaceBidTooLowThrowsException() {
        assertThrows(InvalidBidException.class, () ->
                auctionService.placeBid(testAuctionId, 4L, new BigDecimal("5000000"))); // vứt lỗi khi đặt giá thấp hơn
    }

    @Test // cập nhật người đặt giá cao hơn hiên tại
    @Order(3)
    void testPlaceBidHigherSuccess() throws InvalidBidException, AuctionConnectException, AuctionMisMatchException, AuctionTimeException {
        BidTransaction bid = auctionService.placeBid(testAuctionId, 4L, new BigDecimal("7000000")); // đặt giá mới

        assertNotNull(bid); // ktra đặt tcong chưa
        assertEquals(4L, bid.getBidderId()); // ktra nguời đặt

        Auction updated = auctionDao.findById(testAuctionId).orElseThrow(); // lấy auction sau khi đặt xog
        assertEquals(0, new BigDecimal("7000000").compareTo(updated.getCurrent_price())); // current_price phải bằng giá vừa đặt
        assertEquals(4L, updated.getWinner_bidder_id()); // ktra người dẫn đầu
    }

    @Test // ktra những lần đặt giá trc đc lưu vào db chưa
    @Order(4)
    void testBidHistorySavedToDb() {
        List<BidTransaction> history = auctionService.getBidHistory(testAuctionId); // lấy toàn bộ bid phiên đgia
        assertTrue(history.size() >= 2, "Phải có ít nhất 2 bid trong lịch sử");
        for (int i = 1; i < history.size(); i++) {
            assertFalse(history.get(i).getTimeBidding().isBefore(history.get(i - 1).getTimeBidding()), "Bid sau phải có thời gian >= bid trước"); // ktra thứ tự
        }
    }

    @Test // khi có bid mới thì server đã phát event cho observer đã đki chưa
    @Order(5)
    void testObserverReceivesBidEvent() throws InvalidBidException, AuctionConnectException, AuctionMisMatchException, AuctionTimeException {
        List<AuctionEvent> events = new ArrayList<>(); // ds chứa các event nhận đc
        AuctionObserver observer = events::add; // có event thì observer tự động thêm vào events
        publisher.subscribe(testAuctionId, observer); // observer bắt đầu nghe

        auctionService.placeBid(testAuctionId, 3L, new BigDecimal("8000000")); // đặt giá mơi

        assertFalse(events.isEmpty(), "Observer phải nhận được BID_PLACED event");  // ds ko được rỗng
        assertEquals(AuctionEvent.Type.BID_PLACED, events.get(0).getType()); // event phải là BID_PLACED
        assertEquals(testAuctionId, events.get(0).getAuctionId()); // event phải đúng phiên đgia
        assertEquals(0, new BigDecimal("8000000").compareTo(events.get(0).getCurrentPrice())); // giá trog event là 8 tr

        publisher.unsubscribe(testAuctionId, observer); // hủy theo dõi
    }

    @Test // khi đóng phiên giá thì phải chuyển thành PAID sau khi thanh toán thành công
    @Order(6)
    void testCloseAuction() throws AuctionConnectException, AuctionTimeException {
        auctionService.closeAuction(testAuctionId); // kết thúc phiên

        Auction closed = auctionDao.findById(testAuctionId).orElseThrow(); // lấy dlieu
        assertEquals(AuctionStatus.PAID, closed.getStatus(), "Phiên phải chuyển sang PAID sau closeAuction"); // ktra status

        assertTrue(closed.getWinner_bidder_id() > 0, "Phải có winner sau khi đóng phiên");
    }

    @Test // sau khi kết thúc phiên thì ko ai đặt giá nữa
    @Order(7)
    void testPlaceBidOnClosedAuctionThrowsException() {
        // Phiên đã FINISHED — không thể đặt giá nữa
        assertThrows(AuctionTimeException.class, () ->
                auctionService.placeBid(testAuctionId, 4L, new BigDecimal("9000000"))); // cố đặt giá mới
    }
}
