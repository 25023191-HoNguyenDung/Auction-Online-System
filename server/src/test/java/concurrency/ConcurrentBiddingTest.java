package concurrency;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidDao;
import com.auction.server.dao.jdbc.JdbcAuctionDao;
import com.auction.server.dao.jdbc.JdbcBidDao;
import com.auction.server.dao.jdbc.JdbcUserDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuctionServiceImpl;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
// ktra xử lý đồng thời
class ConcurrentBiddingTest {

    private static AuctionService auctionService;
    private static AuctionDao auctionDao;
    private static BidDao bidDao;
    private static long testAuctionId; // id của phiên đgia test

    @BeforeAll
    static void setUp() throws Exception {
        auctionDao     = new JdbcAuctionDao();
        bidDao         = new JdbcBidDao();
        auctionService = new AuctionServiceImpl(auctionDao, bidDao, new JdbcUserDao());

        Auction auction = new Auction(); // tạo auction mẫu
        // thiết lập thông tin
        auction.setItem_id(1L);
        auction.setSeller_id(2L);
        auction.setStarting_price(new BigDecimal("1000000"));
        auction.setCurrent_price(new BigDecimal("1000000"));
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setStart_time(LocalDateTime.now().minusMinutes(1));
        auction.setEnd_time(LocalDateTime.now().plusHours(1));
        auction.setWinner_bidder_id(0);
        // lưu vào db
        Auction saved = auctionDao.save(auction);
        testAuctionId = saved.getId();
    }

    @AfterAll // test xong thì xóa khỏi db
    static void tearDown() {
        auctionDao.deleteById(testAuctionId);
    }


    @Test // ktra khi có 10 cùng bid
    void testConcurrentBidsNoLostUpdate() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount); // tạo 10 thread
        CountDownLatch latch = new CountDownLatch(1); // tất ca thread chờ ở latch.await();
        AtomicInteger successCount = new AtomicInteger(0); // đếm số lần bid thành công
        AtomicInteger failCount   = new AtomicInteger(0); // đếm số lần bid thất bại

        List<Future<?>> futures = new ArrayList<>(); // lưu kqa các task
        for (int i = 0; i < threadCount; i++) {
            final long bidderId = (i % 2 == 0) ? 3L : 4L;
            final BigDecimal amount = new BigDecimal("1000000").add(new BigDecimal((i + 1) * 100000)); // amount tăng dần theo số lần lặp

            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    auctionService.placeBid(testAuctionId, bidderId, amount); // đặt giá
                    successCount.incrementAndGet(); // thành công
                } catch (Exception e) {
                    failCount.incrementAndGet(); // thất bại
                }
            }));
        }

        latch.countDown(); // tín hiệu bắt đầu
        executor.shutdown(); // ngừng nhận cviec mới
        executor.awaitTermination(10, TimeUnit.SECONDS); // chờ tối đa 10s

        assertTrue(successCount.get() >= 1, "Phải có ít nhất 1 bid thành công"); // ktra có ít nhất 1 bid thành công

        Auction auction = auctionDao.findById(testAuctionId).orElseThrow(); // lấy auction mới nhất
        assertTrue(auction.getCurrent_price().compareTo(new BigDecimal("1000000")) > 0, "Giá phải tăng sau khi có bid"); // giá phải tăng số vơi bđầu

        System.out.println(" Success: " + successCount.get() + " | Failed: " + failCount.get() + " | Final price: " + auction.getCurrent_price()); // in ra kqua
    }

    @Test
    void testNoTwoWinnersAtSameTime() throws InterruptedException { // test hethong có cập nhật dữ liệu chính xác không
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount); // tạo 20 thread
        CountDownLatch latch = new CountDownLatch(1); // tca thread chờ ở latch.await()

        BigDecimal baseAmount = new BigDecimal("2000000"); // giá ban khởi đầu
        for (int i = 0; i < threadCount; i++) {
            final BigDecimal amount = baseAmount.add(new BigDecimal(i * 50000)); // tăng theo từng lượt
            final long bidderId = (i % 2 == 0) ? 3L : 4L; // người đặt là 3 hoặc 4
            executor.submit(() -> {
                try {
                    latch.await(); // chờ tín hiệu bdau
                    auctionService.placeBid(testAuctionId, bidderId, amount); // đặt giá
                } catch (Exception e) {}
            });
        }

        latch.countDown(); // 20 thread đặt giá đồng thơi
        executor.shutdown(); // ko nhận việc mới
        executor.awaitTermination(10, TimeUnit.SECONDS); // chờ tối đa 10s

        Auction auction = auctionDao.findById(testAuctionId).orElseThrow(); // lấy acution
        List<BidTransaction> bids = bidDao.findByAuctionId(testAuctionId); // lấy lsu bid

        BidTransaction highest = bidDao.findHighestBidByAuctionId(testAuctionId).orElseThrow(); // lấy bid cao nhất
        assertTrue(auction.getCurrent_price().compareTo(highest.getBidAmount()) >= 0, "Giá DB phải khớp với bid cao nhất"); // bid mới nhất == bid cao nhất

        System.out.println(" Winner: " + auction.getWinner_bidder_id() + " | Highest bid: " + highest.getBidAmount() + " | Total bids: " + bids.size());
    }
}