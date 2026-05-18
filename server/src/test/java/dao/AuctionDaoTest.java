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
// đảm bảo tất cả thao tác với db
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionDaoTest {

    private static AuctionDao dao;
    private static long savedId; // lưu id auction vừa tạo

    @BeforeAll
    static void setUp() {
        dao = new JdbcAuctionDao();
    }

    @Test // ktra lưu auction vào db
    @Order(1) // chạy đầu tiên
    void testSave() throws AuctionConnectException, SQLException {
        Auction auction = new Auction(); // tạo 1 phiên đgia
        // khởi tạo gtri bđầu
        auction.setItem_id(1L);
        auction.setSeller_id(2L);
        auction.setStarting_price(new BigDecimal("10000000"));
        auction.setCurrent_price(new BigDecimal("10000000"));
        auction.setStatus(AuctionStatus.OPEN);
        auction.setStart_time(LocalDateTime.now());
        auction.setEnd_time(LocalDateTime.now().plusDays(1));

        Auction saved = dao.save(auction); // lưu vào db
        savedId = saved.getId(); // lấy id phiên đgia

        assertTrue(saved.getId() > 0, "ID phải được gán sau khi save"); // ktra id>0
    }

    @Test // tìm auction = id
    @Order(2) // chạy thứ 2
    void testFindById() {
        Optional<Auction> found = dao.findById(savedId); // tìm trong db
        assertTrue(found.isPresent()); //ktra có tìm thấy ko
        assertEquals(AuctionStatus.OPEN, found.get().getStatus()); //ktra xem có đag OPEN ko
        assertEquals(2L, found.get().getSeller_id()); // ktra đúng người bán ko
    }

    @Test // ktra có update dlieu trong db không
    @Order(3)
    void testUpdate() throws AuctionConnectException, SQLException {
        Auction auction = dao.findById(savedId).orElseThrow(); // tìm auction đã lưu trc đó
        // sửa dlieu
        auction.setCurrent_price(new BigDecimal("12000000"));
        auction.setStatus(AuctionStatus.RUNNING);

        dao.update(auction); // lưu thay đổi vào db

        Auction updated = dao.findById(savedId).orElseThrow(); // lấy lại từ db
        assertEquals(AuctionStatus.RUNNING, updated.getStatus()); // có đag RUNNING ko
        assertEquals(0, new BigDecimal("12000000").compareTo(updated.getCurrent_price())); // giá mới update chưa
    }

    @Test // có tìm đc theo status ko
    @Order(4)
    void testFindByStatus() {
        List<Auction> list = dao.findByStatus(AuctionStatus.RUNNING); // tìm = status
        assertFalse(list.isEmpty()); //
    }

    @Test // xem có lấy đc toàn bộ dlieu bang auction ko
    @Order(5)
    void testFindAll() {
        List<Auction> list = dao.findAll();
        assertTrue(list.size() >= 1);
    }

    @Test
    @Order(6) // xem đag running mà hết tg đgia
    void testFindExpiredRunning() {
        List<Auction> expired = dao.findExpiredRunning(); // lấy các phiên đag running nhưng đã quá end_time
        assertNotNull(expired); // ktra xem ds có null ko
        expired.forEach(a -> { // duyệt từng auction trong ds
            assertEquals(AuctionStatus.RUNNING, a.getStatus()); // đảm bảo mỗi auction đag running
            assertTrue(a.getEnd_time().isBefore(LocalDateTime.now())); // đảm bảo endtime trc thời điểm htai
        });
    }

    @Test // ktra xem đã xóa khỏi db chưa
    @Order(7)
    void testDeleteById() {
        boolean deleted = dao.deleteById(savedId);
        assertTrue(deleted); // ktra đã xóa chưa
        assertFalse(dao.findById(savedId).isPresent()); // xem còn trog db ko
    }
}
