package com.auction.server.dao.jdbc;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.AutoBidProfileDao;
import com.auction.server.model.AutoBidProfile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAutoBidProfileDao implements AutoBidProfileDao {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private AutoBidProfile mapRow(ResultSet rs) throws SQLException {
        AutoBidProfile profile = new AutoBidProfile();
        profile.setId(rs.getLong("id"));
        profile.setUser_id(rs.getLong("user_id"));
        profile.setAuction_id(rs.getLong("auction_id"));
        profile.setMax_bid(rs.getBigDecimal("max_bid"));
        profile.setIncrement(rs.getBigDecimal("increment"));
        profile.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
        return profile;
    }

    @Override
    public Optional<AutoBidProfile> findById(long id) {
        String sql = "SELECT * FROM auto_bid_profiles WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById auto_bid: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<AutoBidProfile> findByAuctionId(long auctionId) {
        // Ai đăng trước → ưu tiên trước
        String sql = """
            SELECT * FROM auto_bid_profiles
            WHERE auction_id = ?
            ORDER BY created_at ASC
        """;
        List<AutoBidProfile> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByAuctionId auto_bid: " + auctionId, e);
        }
        return list;
    }

    @Override
    public Optional<AutoBidProfile> findByUserIdAndAuctionId(long userId, long auctionId) {
        String sql = "SELECT * FROM auto_bid_profiles WHERE user_id = ? AND auction_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByUserIdAndAuctionId auto_bid", e);
        }
        return Optional.empty();
    }

    @Override
    public AutoBidProfile save(AutoBidProfile profile) {

        String sql = """
            INSERT INTO auto_bid_profiles (user_id, auction_id, max_bid, increment)
            VALUES (?, ?, ?, ?)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, profile.getUser_id());
            ps.setLong(2, profile.getAuction_id());
            ps.setBigDecimal(3, profile.getMax_bid());
            ps.setBigDecimal(4, profile.getIncrement());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) profile.setId(keys.getLong(1));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save auto_bid profile", e);
        }
        return profile;
    }

    @Override
    public AutoBidProfile update(AutoBidProfile profile) {

        String sql = """
            UPDATE auto_bid_profiles
            SET max_bid = ?, increment = ?
            WHERE id = ?
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, profile.getMax_bid());
            ps.setBigDecimal(2, profile.getIncrement());
            ps.setLong(3, profile.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi update auto_bid profile id: " + profile.getId(), e);
        }
        return profile;
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM auto_bid_profiles WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi deleteById auto_bid: " + id, e);
        }
    }
}
