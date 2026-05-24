package com.auction.server.dao.jdbc;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.BidDao;
import com.auction.server.model.Bidder;
import com.auction.server.model.BidTransaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
// làm việc trực tiếp với db
public class JdbcBidDao implements BidDao {

    private final DatabaseConfig db = DatabaseConfig.getInstance();
    // biến dlieu trong db thành obj trong java
    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        long id        = rs.getLong("bidId");
        long auctionId = rs.getLong("auctionId");
        long bidderId  = rs.getLong("bidder");
        java.math.BigDecimal amount = rs.getBigDecimal("amount");
        java.time.LocalDateTime bidTime = rs.getTimestamp("bid_time").toLocalDateTime();
        
        String username = "";
        try {
            username = rs.getString("user_name");
        } catch (SQLException ignored) {}
        
        Bidder bidder = new Bidder(username, bidderId, "", "", "BIDDER", BigDecimal.ZERO, new ArrayList<>());
        return new BidTransaction(id, auctionId, bidder, amount, bidTime);
    }

    @Override
    public Optional<BidTransaction> findById(long id) {
        String sql = "SELECT * FROM bids WHERE bidId = ?";   // bidId
        // kết nối db
        try (Connection conn = db.getConnection();
             // chuẩn bị câu SQL + chờ gắn dữ liệu vào
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery(); // chạy câu lệnh SQL và lấy kq trả về từ db
            if (rs.next()) return Optional.of(mapRow(rs)); // nếu có dlieu trả về thì chuyển thành obj
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById bid: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<BidTransaction> findByAuctionId(long auctionId) {
        String sql = "SELECT b.*, u.user_name FROM bids b JOIN users u ON b.bidder = u.id WHERE b.auctionId = ? ORDER BY b.bid_time ASC"; // auctionId
        List<BidTransaction> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByAuctionId: " + auctionId, e);
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
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findHighestBid auction: " + auctionId, e);
        }
        return Optional.empty();
    }

    @Override
    public BidTransaction save(BidTransaction bid) {
        String sql = "INSERT INTO bids (auctionId, bidder, amount) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, bid.getAuctionId());
            ps.setLong(2, bid.getBidderId());
            ps.setBigDecimal(3, bid.getBidAmount());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) bid.setId(keys.getLong(1));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save bid", e);
        }
        return bid;
    }
}