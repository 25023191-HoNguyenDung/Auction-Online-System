package com.auction.server.dao.jdbc;

import com.auction.server.concurrency.ConnectionHolder;
import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.BidDao;
import com.auction.server.model.Bidder;
import com.auction.server.model.BidTransaction;

import java.math.BigDecimal;
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
 * PATH: server/src/main/java/com/auction/server/dao/jdbc/JdbcBidDao.java
 */
public class JdbcBidDao implements BidDao {

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

    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        long id        = rs.getLong("bidId");
        long auctionId = rs.getLong("auctionId");
        long bidderId  = rs.getLong("bidder");
        BigDecimal amount = rs.getBigDecimal("amount");
        java.time.LocalDateTime bidTime = rs.getTimestamp("bid_time").toLocalDateTime();

        String username = "";
        try { username = rs.getString("user_name"); } catch (SQLException ignored) {}

        Bidder bidder = new Bidder(username, bidderId, "", "", "BIDDER", BigDecimal.ZERO, new ArrayList<>());
        return new BidTransaction(id, auctionId, bidder, amount, bidTime);
    }

    @Override
    public Optional<BidTransaction> findById(long id) {
        String sql = "SELECT * FROM bids WHERE bidId = ?";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById bid: " + id, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<BidTransaction> findByAuctionId(long auctionId) {
        String sql = "SELECT b.*, u.user_name FROM bids b JOIN users u ON b.bidder = u.id WHERE b.auctionId = ? ORDER BY b.bid_time ASC";
        List<BidTransaction> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, auctionId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByAuctionId: " + auctionId, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return list;
    }

    @Override
    public Optional<BidTransaction> findHighestBidByAuctionId(long auctionId) {
        String sql = """
            SELECT * FROM bids
            WHERE auctionId = ?
            ORDER BY amount DESC, bid_time ASC
            LIMIT 1
        """;
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, auctionId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findHighestBid auction: " + auctionId, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return Optional.empty();
    }

    @Override
    public BidTransaction save(BidTransaction bid) {
        String sql = "INSERT INTO bids (auctionId, bidder, amount) VALUES (?, ?, ?)";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, bid.getAuctionId());
                ps.setLong(2, bid.getBidderId());
                ps.setBigDecimal(3, bid.getBidAmount());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) bid.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save bid", e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return bid;
    }

    private void closeQuietly(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignored) {}
    }
}