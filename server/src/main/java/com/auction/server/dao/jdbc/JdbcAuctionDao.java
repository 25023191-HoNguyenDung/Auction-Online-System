package com.auction.server.dao.jdbc;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.auction.common.exception.AuctionConnectException;
import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.AuctionDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
// làm việc trực tiếp với db
public class JdbcAuctionDao implements AuctionDao {
    // knoi tới db
    private final DatabaseConfig db = DatabaseConfig.getInstance();
    // biến dlieu trong db thành obj trong java
    private Auction mapRow(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getLong("id"));
        auction.setItem_id(rs.getLong("item_id"));
        auction.setSeller_id(rs.getLong("seller_id"));
        auction.setStarting_price(rs.getBigDecimal("starting_price"));
        auction.setCurrent_price(rs.getBigDecimal("current_price"));
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        auction.setStart_time(rs.getTimestamp("start_time").toLocalDateTime());
        auction.setEnd_time(rs.getTimestamp("end_time").toLocalDateTime());
        long winnerId = rs.getLong("winner_bidder_id");
        auction.setWinner_bidder_id(rs.wasNull() ? 0L : winnerId);
        return auction;
    }

    @Override // tìm theo id
    public Optional<Auction> findById(long id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = db.getConnection();
             // chuẩn bị câu SQL + chờ gắn dữ liệu vào
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery(); // chạy SQL và lấy kq trả về từ db
            if (rs.next()) return Optional.of(mapRow(rs)); // nếu có dữ liệu thì chuyển về obj item
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById auction: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Auction> findByStatus(AuctionStatus status) {
        String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY end_time ASC";
        List<Auction> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             // chuẩn bị câu SQL + chờ gắn dữ liệu vào
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery(); // chạy và lấy dlieu từ db về
            while (rs.next()) list.add(mapRow(rs)); // nếu dlieu có thì thêm vào list
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByStatus: " + status, e);
        }
        return list;
    }

    @Override
    public List<Auction> findAll() {
        String sql = "SELECT * FROM auctions ORDER BY id ASC";
        List<Auction> list = new ArrayList<>(); // ds đựng auction
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement(); // ko có biến truyền vào lên dùng Statement th
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs)); // nếu có thì add vào list
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findAll auctions", e);
        }
        return list;
    }

    @Override
    public List<Auction> findExpiredRunning() {
        String sql = "SELECT * FROM auctions WHERE status = 'RUNNING' AND end_time <= NOW()";
        List<Auction> list = new ArrayList<>(); // ds đựng auction
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement(); // ko có biến truyền vào lên dùng Statement th
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs)); // nếu có thì add vào list
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findExpiredRunning", e);
        }
        return list;
    }

    @Override
    public Auction save(Auction auction) throws AuctionConnectException {
        String sql = """
            INSERT INTO auctions
                (item_id, seller_id, starting_price, current_price, status, start_time, end_time)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // thay vào ?
            ps.setLong(1, auction.getItem_id());
            ps.setLong(2, auction.getSeller_id());
            ps.setBigDecimal(3, auction.getStarting_price());
            ps.setBigDecimal(4, auction.getCurrent_price());
            ps.setString(5, auction.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(auction.getStart_time()));
            ps.setTimestamp(7, Timestamp.valueOf(auction.getEnd_time()));
            ps.executeUpdate(); // gửi câu lệnh SQL xuống db
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) auction.setId(keys.getLong(1)); // ktra xem có id ko, có thì gắn vào obj
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save auction", e);
        }
        return auction;
    }

    @Override
    public Auction update(Auction auction) throws AuctionConnectException {
        String sql = """
            UPDATE auctions
            SET current_price = ?, status = ?, end_time = ?, winner_bidder_id = ?
            WHERE id = ?
        """;
        try (Connection conn = db.getConnection();
             // tạo câu sql an toàn
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, auction.getCurrent_price());
            ps.setString(2, auction.getStatus().name());
            ps.setTimestamp(3, Timestamp.valueOf(auction.getEnd_time()));
            if (auction.getWinner_bidder_id() != 0) {
                ps.setLong(4, auction.getWinner_bidder_id());
            } else {
                ps.setNull(4, Types.BIGINT);

            }
            ps.setLong(5, auction.getId());
            ps.executeUpdate(); // gửi câu lệnh SQL xuống db
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi update auction id: " + auction.getId(), e);
        }
        return auction;
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0; //số dòng trong database bị thay đổi
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi deleteById auction: " + id, e);
        }
    }

    @Override
    public List<Auction> findOpenReadyToStart() {
        String sql = "SELECT * FROM auctions WHERE status = 'OPEN' AND start_time <= NOW()";
        List<Auction> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findOpenReadyToStart", e);
        }
        return list;
    }
}