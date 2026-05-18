package com.auction.server.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.TransactionDao;
import com.auction.server.model.Transaction;

public class JdbcTransactionDao implements TransactionDao {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private Transaction mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("transaction_id");
        long userId = rs.getLong("user_id");
        String type = rs.getString("transaction_type");
        double amount = rs.getDouble("amount");
        Long auctionId = rs.getObject("auction_id") != null ? rs.getLong("auction_id") : null;
        String description = rs.getString("description");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        return new Transaction(id, userId, type, amount, auctionId, description, createdAt);
    }

    @Override
    public Transaction save(Transaction transaction) {
        String sql = "INSERT INTO transactions (user_id, transaction_type, amount, auction_id, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, transaction.getUserId());
            ps.setString(2, transaction.getTransactionType());
            ps.setDouble(3, transaction.getAmount());
            if (transaction.getAuctionId() != null) {
                ps.setLong(4, transaction.getAuctionId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setString(5, transaction.getDescription());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) transaction.setId(keys.getLong(1));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save transaction: " + transaction, e);
        }
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(long id) {
        String sql = "SELECT * FROM transactions WHERE transaction_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById transaction: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Transaction> findByUserId(long userId) {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_at DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByUserId transaction: " + userId, e);
        }
        return list;
    }

    @Override
    public List<Transaction> findByAuctionId(long auctionId) {
        String sql = "SELECT * FROM transactions WHERE auction_id = ? ORDER BY created_at DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByAuctionId transaction: " + auctionId, e);
        }
        return list;
    }

    @Override
    public List<Transaction> findAll() {
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findAll transactions", e);
        }
        return list;
    }
}
