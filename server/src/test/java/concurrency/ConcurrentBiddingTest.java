package concurrency;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidDao;
import com.auction.server.dao.jdbc.JdbcAuctionDao;
import com.auction.server.dao.jdbc.JdbcBidDao;
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

class ConcurrentBiddingTest {

    private static AuctionService auctionService;
    private static AuctionDao auctionDao;
    private static BidDao bidDao;
    private static long testAuctionId;

    @BeforeAll
    static void setUp() throws Exception {
        auctionDao     = new JdbcAuctionDao();
        bidDao         = new JdbcBidDao();
        auctionService = new AuctionServiceImpl(auctionDao, bidDao, new com.auction.server.dao.jdbc.JdbcUserDao());

        Auction auction = new Auction();
        auction.setItem_id(1L);
        auction.setSeller_id(2L);
        auction.setStarting_price(new BigDecimal("1000000"));
        auction.setCurrent_price(new BigDecimal("1000000"));
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setStart_time(LocalDateTime.now().minusMinutes(1));
        auction.setEnd_time(LocalDateTime.now().plusHours(1));
        auction.setWinner_bidder_id(0);

        Auction saved = auctionDao.save(auction);
        testAuctionId = saved.getId();
    }

    @AfterAll
    static void tearDown() {
        auctionDao.deleteById(testAuctionId);
    }


    @Test
    void testConcurrentBidsNoLostUpdate() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount   = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final long bidderId = (i % 2 == 0) ? 3L : 4L;
            final BigDecimal amount = new BigDecimal("1000000")
                    .add(new BigDecimal((i + 1) * 100000));

            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    auctionService.placeBid(testAuctionId, bidderId, amount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(successCount.get() >= 1, "Phải có ít nhất 1 bid thành công");

        Auction auction = auctionDao.findById(testAuctionId).orElseThrow();
        assertTrue(auction.getCurrent_price().compareTo(new BigDecimal("1000000")) > 0,
                "Giá phải tăng sau khi có bid");

        System.out.println("✓ Success: " + successCount.get() + " | Failed: " + failCount.get()
                + " | Final price: " + auction.getCurrent_price());
    }

    @Test
    void testNoTwoWinnersAtSameTime() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        BigDecimal baseAmount = new BigDecimal("2000000");
        for (int i = 0; i < threadCount; i++) {
            final BigDecimal amount = baseAmount.add(new BigDecimal(i * 50000));
            final long bidderId = (i % 2 == 0) ? 3L : 4L;
            executor.submit(() -> {
                try {
                    latch.await();
                    auctionService.placeBid(testAuctionId, bidderId, amount);
                } catch (Exception ignored) {}
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Auction auction = auctionDao.findById(testAuctionId).orElseThrow();
        List<BidTransaction> bids = bidDao.findByAuctionId(testAuctionId);

        BidTransaction highest = bidDao.findHighestBidByAuctionId(testAuctionId).orElseThrow();
        assertEquals(0, auction.getCurrent_price().compareTo(highest.getBidAmount()),
                "Giá DB phải khớp với bid cao nhất");

        System.out.println("✓ Winner: " + auction.getWinner_bidder_id()
                + " | Highest bid: " + highest.getBidAmount()
                + " | Total bids: " + bids.size());
    }
}