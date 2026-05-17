package dao;

import com.auction.common.exception.AuctionConnectException;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.jdbc.JdbcAuctionDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionDaoTest {

    private static AuctionDao dao;
    private static long savedId;

    @BeforeAll
    static void setUp() {
        dao = new JdbcAuctionDao();
    }

    @Test
    @Order(1)
    void testSave() throws AuctionConnectException, SQLException {
        Auction auction = new Auction();
        auction.setItem_id(1L);
        auction.setSeller_id(2L);
        auction.setStarting_price(new BigDecimal("10000000"));
        auction.setCurrent_price(new BigDecimal("10000000"));
        auction.setStatus(AuctionStatus.OPEN);
        auction.setStart_time(LocalDateTime.now());
        auction.setEnd_time(LocalDateTime.now().plusDays(1));

        Auction saved = dao.save(auction);
        savedId = saved.getId();

        assertTrue(saved.getId() > 0, "ID phải được gán sau khi save");
    }

    @Test
    @Order(2)
    void testFindById() {
        Optional<Auction> found = dao.findById(savedId);
        assertTrue(found.isPresent());
        assertEquals(AuctionStatus.OPEN, found.get().getStatus());
        assertEquals(2L, found.get().getSeller_id());
    }

    @Test
    @Order(3)
    void testUpdate() throws AuctionConnectException, SQLException {
        Auction auction = dao.findById(savedId).orElseThrow();
        auction.setCurrent_price(new BigDecimal("12000000"));
        auction.setStatus(AuctionStatus.RUNNING);

        dao.update(auction);

        Auction updated = dao.findById(savedId).orElseThrow();
        assertEquals(AuctionStatus.RUNNING, updated.getStatus());
        assertEquals(0, new BigDecimal("12000000").compareTo(updated.getCurrent_price()));
    }

    @Test
    @Order(4)
    void testFindByStatus() {
        List<Auction> list = dao.findByStatus(AuctionStatus.RUNNING);
        assertFalse(list.isEmpty());
    }

    @Test
    @Order(5)
    void testFindAll() {
        List<Auction> list = dao.findAll();
        assertTrue(list.size() >= 1);
    }

    @Test
    @Order(6)
    void testFindExpiredRunning() {
        List<Auction> expired = dao.findExpiredRunning();
        assertNotNull(expired);
        // Tất cả phải là RUNNING và end_time đã qua
        expired.forEach(a -> {
            assertEquals(AuctionStatus.RUNNING, a.getStatus());
            assertTrue(a.getEnd_time().isBefore(LocalDateTime.now()));
        });
    }

    @Test
    @Order(7)
    void testDeleteById() {
        boolean deleted = dao.deleteById(savedId);
        assertTrue(deleted);
        assertFalse(dao.findById(savedId).isPresent());
    }
}
