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
import org.junit.jupiter.api.parallel.Isolated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Isolated
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConcurrentBiddingTest {


    private static final long SELLER_ID   = 2L;
    private static final long ITEM_ID     = 1L;

    // Hai bidder luân phiên đặt giá trong các test
    private static final long BIDDER_A    = 3L;
    private static final long BIDDER_B    = 4L;

    // Timeout tối đa cho mỗi test
    private static final int  TIMEOUT_SEC = 30;

    // Giá khởi điểm dùng chung cho cả hai auction
    private static final BigDecimal START_PRICE = new BigDecimal("1000000");


    private static AuctionService auctionService;
    private static AuctionDao     auctionDao;
    private static BidDao         bidDao;

    // Mỗi test dùng auction riêng.
    private static long auctionId1 = 0;
    private static long auctionId2 = 0;


    @BeforeAll
    static void setUp() throws Exception {
        auctionDao     = new JdbcAuctionDao();
        bidDao         = new JdbcBidDao();
        auctionService = new AuctionServiceImpl(auctionDao, bidDao, new JdbcUserDao());

        // Tạo hai auction độc lập trước khi chạy test
        auctionId1 = auctionDao.save(buildAuction(START_PRICE)).getId();
        auctionId2 = auctionDao.save(buildAuction(START_PRICE)).getId();

        // check knoi tới cơ sở dữ liệu chưa
        assertTrue(auctionId1 > 0, "Không tạo được auction 1 — kiểm tra kết nối DB và seed data");
        assertTrue(auctionId2 > 0, "Không tạo được auction 2 — kiểm tra kết nối DB và seed data");
    }

    @AfterAll
    static void tearDown() {
        // dọn dẹp dữ liệu sau khi test xong
        if (auctionId1 > 0) auctionDao.deleteById(auctionId1);
        if (auctionId2 > 0) auctionDao.deleteById(auctionId2);
    }
    // tạo đtg auction mẫu test
    private static Auction buildAuction(BigDecimal startPrice) {
        Auction a = new Auction();
        a.setItem_id(ITEM_ID);
        a.setSeller_id(SELLER_ID);
        a.setStarting_price(startPrice);
        a.setCurrent_price(startPrice);
        a.setStatus(AuctionStatus.RUNNING);
        a.setStart_time(LocalDateTime.now().minusMinutes(5));
        a.setEnd_time(LocalDateTime.now().plusHours(2));
        a.setWinner_bidder_id(0L);
        return a;
    }

    // tạo nhiều thread cùng lúc đặt bid xem có concurrency an toàn không
    private int[] runConcurrentBids(long auctionId, int threadCount, BigDecimal baseAmount, int stepSize) throws InterruptedException {

        ExecutorService executor  = Executors.newFixedThreadPool(threadCount);
        CountDownLatch  startGate = new CountDownLatch(1); // tất cả thread chờ await()
        CountDownLatch  doneLatch = new CountDownLatch(threadCount); // chờ tất cả thread hoàn thành

        AtomicInteger successCount = new AtomicInteger(0); // đếm số lần thành công
        AtomicInteger failCount    = new AtomicInteger(0); // đếm số lần thất bại
        // vòng lặp tạo thread
        for (int i = 0; i < threadCount; i++) {
            final long       bidderId = (i % 2 == 0) ? BIDDER_A : BIDDER_B; // luân phiên
            final BigDecimal amount   = baseAmount.add(new BigDecimal((i + 1) * stepSize)); // tính bid
            // đặt bid
            executor.submit(() -> {
                try {
                    startGate.await(); // tca thread dừng ở đây
                    auctionService.placeBid(auctionId, bidderId, amount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown(); // tất cả thread ht mới đc tiếp tục
                }
            });
        }

        startGate.countDown(); // tất cả bắt đầu cùng lúc
        boolean finished = doneLatch.await(TIMEOUT_SEC, TimeUnit.SECONDS); // true: đúng hạn/false: bị treo/deadlock
        executor.shutdownNow();

        assertTrue(finished, "Các thread không hoàn thành trong " + TIMEOUT_SEC + " giây — kiểm tra deadlock hoặc tăng TIMEOUT_SEC");

        return new int[]{ successCount.get(), failCount.get() };
    }
    // check bị lost update khi nhiều ng cùng đặt không
    @Test
    @Order(1)
    void testConcurrentBids_noLostUpdate() throws InterruptedException {
        int[] counts = runConcurrentBids(auctionId1, 10, START_PRICE, 100_000); // mô phỏng đgia xem bnhieu tcong tbai
        int successCount = counts[0];
        int failCount    = counts[1];

        assertTrue(successCount >= 1, "Phải có ít nhất 1 bid thành công. success=" + successCount + ", fail=" + failCount);

        Auction finalAuction = auctionDao.findById(auctionId1).orElseThrow(); // lấy trạng thái cuối cùng của auction
        BidTransaction highestBid   = bidDao.findHighestBidByAuctionId(auctionId1).orElseThrow(); // lấy bid max
        // giá phải tăng
        assertTrue(finalAuction.getCurrent_price().compareTo(START_PRICE) > 0, "Giá cuối phải cao hơn giá khởi điểm");
        // giá htai = gia cao nhất
        assertEquals(0, finalAuction.getCurrent_price().compareTo(highestBid.getBidAmount()), "current_price trong DB phải bằng đúng bid cao nhất — " + "nếu fail thì có race condition trong AuctionLogicManager");
        System.out.printf("[Test1] success=%d | fail=%d | finalPrice=%s | highestBid=%s%n", successCount, failCount, finalAuction.getCurrent_price(), highestBid.getBidAmount());
    }
    // nhiều ng cùng bid có winner có đúng không
    @Test
    @Order(2)
    void testConcurrentBids_singleWinner() throws InterruptedException {
        int[] counts = runConcurrentBids(auctionId2, 20, START_PRICE, 50_000); // mô phỏng đgia
        int successCount = counts[0]; // số lần tcong

        Auction finalAuction = auctionDao.findById(auctionId2).orElseThrow(); // lấy trạng thái cuối cùng
        BidTransaction highestBid = bidDao.findHighestBidByAuctionId(auctionId2).orElseThrow(); // bid max
        List<BidTransaction> allBids = bidDao.findByAuctionId(auctionId2); // lấy tất cả bid đã lưu
        // ktra người thắng
        assertEquals(highestBid.getBidderId(), finalAuction.getWinner_bidder_id(), "winner_bidder_id phải là người đặt bid cao nhất — " + "nếu fail thì updateAuctionAfterBid có race condition");
        // ktra lần 2
        assertEquals(0, finalAuction.getCurrent_price().compareTo(highestBid.getBidAmount()), "current_price phải bằng đúng bid cao nhất trong bảng bids");
        // ktra số bid tcong với số bid trong db
        assertEquals(successCount, allBids.size(), "Số bid trong DB phải khớp số lần placeBid thành công — " + "nếu fail thì có bid bị drop hoặc duplicate");
        System.out.printf("[Test2] success=%d | winner=%d | finalPrice=%s | totalBids=%d%n", successCount, finalAuction.getWinner_bidder_id(), finalAuction.getCurrent_price(), allBids.size());}
}
