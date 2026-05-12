package com.auction.server.dao.jdbc;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.BidDao;
import com.auction.server.model.Bidder;
import com.auction.server.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBidDao implements BidDao {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        long id        = rs.getLong("bidId");        // đổi id → bidId
        long auctionId = rs.getLong("auctionId");    // đổi auction_id → auctionId
        long bidderId  = rs.getLong("bidder");       // đổi bidder_id → bidder
        java.math.BigDecimal amount = rs.getBigDecimal("amount");
        java.time.LocalDateTime bidTime = rs.getTimestamp("bid_time").toLocalDateTime();

        Bidder bidder = new Bidder(
                "",
                bidderId,
                "", "", "BIDDER",
                0.0,
                new ArrayList<>()
        );
        return new BidTransaction(id, auctionId, bidder, amount, bidTime);
    }

    @Override
    public Optional<BidTransaction> findById(long id) {
        String sql = "SELECT * FROM bids WHERE bidId = ?";   // bidId
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById bid: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<BidTransaction> findByAuctionId(long auctionId) {
        String sql = "SELECT * FROM bids WHERE auctionId = ? ORDER BY bid_time ASC"; // auctionId
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