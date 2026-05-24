package com.auction.server.dao.jdbc;

import com.auction.server.concurrency.ConnectionHolder;
import com.auction.common.exception.AuctionConnectException;
import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.AuctionDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PHIÊN BẢN ĐÃ SỬA:
 * - Thêm getConn() / shouldClose() để ưu tiên dùng connection từ transaction (ConnectionHolder)
 * - Khi trong transaction: KHÔNG close connection sau mỗi câu SQL
 * - Khi ngoài transaction: tự mở + close connection như cũ
 *
 * PATH: server/src/main/java/com/auction/server/dao/jdbc/JdbcAuctionDao.java
 */
public class JdbcAuctionDao implements AuctionDao {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    // Lấy connection: ưu tiên connection của transaction nếu đang có
    private Connection getConn() throws SQLException {
        Connection tx = ConnectionHolder.get();
        return (tx != null) ? tx : db.getConnection();
    }

    // Kiểm tra có cần close connection sau khi dùng không
    private boolean shouldClose() {
        return ConnectionHolder.get() == null;
    }

    private void closeQuietly(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignored) {}
    }

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

    @Override
    public Optional<Auction> findById(long id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById auction: " + id, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<Auction> findByStatus(AuctionStatus status) {
        String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY end_time ASC";
        List<Auction> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status.name());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByStatus: " + status, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return list;
    }

    @Override
    public List<Auction> findAll() {
        String sql = "SELECT * FROM auctions ORDER BY id ASC";
        List<Auction> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConn();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findAll auctions", e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return list;
    }

    @Override
    public List<Auction> findExpiredRunning() {
        String sql = "SELECT * FROM auctions WHERE status = 'RUNNING' AND end_time <= UTC_TIMESTAMP()";
        List<Auction> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findExpiredRunning", e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
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
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, auction.getItem_id());
                ps.setLong(2, auction.getSeller_id());
                ps.setBigDecimal(3, auction.getStarting_price());
                ps.setBigDecimal(4, auction.getCurrent_price());
                ps.setString(5, auction.getStatus().name());
                ps.setTimestamp(6, Timestamp.valueOf(auction.getStart_time()));
                ps.setTimestamp(7, Timestamp.valueOf(auction.getEnd_time()));
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) auction.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save auction", e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
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
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBigDecimal(1, auction.getCurrent_price());
                ps.setString(2, auction.getStatus().name());
                ps.setTimestamp(3, Timestamp.valueOf(auction.getEnd_time()));
                if (auction.getWinner_bidder_id() != 0) {
                    ps.setLong(4, auction.getWinner_bidder_id());
                } else {
                    ps.setNull(4, Types.BIGINT);
                }
                ps.setLong(5, auction.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi update auction id: " + auction.getId(), e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return auction;
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi deleteById auction: " + id, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
    }

    @Override
    public List<Auction> findOpenReadyToStart() {
        // Trả về danh sách rỗng để ngăn luồng Scheduler tự động kích hoạt phiên đấu giá
        // đang ở trạng thái OPEN (chờ duyệt). Phiên đấu giá chỉ được phép chuyển sang RUNNING
        // khi và chỉ khi Admin phê duyệt thủ công.
        return new ArrayList<>();
    }
}