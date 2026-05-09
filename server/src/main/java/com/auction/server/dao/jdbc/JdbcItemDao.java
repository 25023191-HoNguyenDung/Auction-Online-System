package com.auction.server.dao.jdbc;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.ItemDao;
import com.auction.server.model.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcItemDao implements ItemDao {
    private final DatabaseConfig db = DatabaseConfig.getInstance();
    private Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setId(rs.getLong("id"));
        item.setSellerId(rs.getLong("seller_id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setCategory(rs.getString("category"));
        item.setStartingPrice(rs.getBigDecimal("starting_price"));
        item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return item;
    }

    @Override
    public Optional<Item> findById(long id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById item: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Item> findBySellerId(long sellerId) {
        String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY created_at DESC";
        List<Item> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findBySellerId: " + sellerId, e);
        }
        return list;
    }

    @Override
    public List<Item> findAll() {
        String sql = "SELECT * FROM items ORDER BY created_at DESC";
        List<Item> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findAll items", e);
        }
        return list;
    }

    @Override
    public Item save(Item item) {
        String sql = """
            INSERT INTO items (seller_id, name, description, category, starting_price)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, item.getSellerId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getCategory());
            ps.setBigDecimal(5, item.getStartingPrice());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) item.setId(keys.getLong(1));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save item: " + item.getName(), e);
        }
        return item;
    }

    @Override
    public Item update(Item item) {
        String sql = """
            UPDATE items
            SET name = ?, description = ?, category = ?, starting_price = ?
            WHERE id = ?
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getCategory());
            ps.setBigDecimal(4, item.getStartingPrice());
            ps.setLong(5, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi update item id: " + item.getId(), e);
        }
        return item;
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi deleteById item: " + id, e);
        }
    }
}
