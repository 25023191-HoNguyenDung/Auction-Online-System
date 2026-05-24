package com.auction.server.dao.jdbc;

import com.auction.server.concurrency.ConnectionHolder;
import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.UserDao;
import com.auction.server.model.Admin;
import com.auction.server.model.Bidder;
import com.auction.server.model.Seller;
import com.auction.server.model.User;

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
 * PATH: server/src/main/java/com/auction/server/dao/jdbc/JdbcUserDao.java
 */
public class JdbcUserDao implements UserDao {

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

    private User mapRow(ResultSet rs) throws SQLException {
        long id       = rs.getLong("id");
        String username = rs.getString("user_name");
        String email    = rs.getString("email");
        String password = rs.getString("password");
        String role     = rs.getString("role");
        return switch (role.toUpperCase()) {
            case "BIDDER" -> new Bidder(username, id, email, password, role, rs.getBigDecimal("account_balance"), new ArrayList<>());
            case "SELLER" -> new Seller(username, id, email, password, role, rs.getBigDecimal("account_balance"), new ArrayList<>(), new ArrayList<>());
            case "ADMIN"  -> new Admin(username, id, email, password, role);
            default -> throw new RuntimeException("Role không hợp lệ: " + role);
        };
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById user: " + id, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE user_name = ?";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByUsername: " + username, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id ASC";
        List<User> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConn();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findAll users", e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return list;
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (user_name, password, email, role, account_balance) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.get_user_name());
                ps.setString(2, user.get_password());
                ps.setString(3, user.get_email());
                ps.setString(4, user.getRole());
                if (user instanceof Seller seller) {
                    ps.setBigDecimal(5, seller.getAccount_balance());
                } else if (user instanceof Bidder bidder) {
                    ps.setBigDecimal(5, bidder.getAccount_balance());
                } else {
                    ps.setBigDecimal(5, null);
                }
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) user.set_ID(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save user: " + user.get_user_name(), e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, password = ?, account_balance = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.get_email());
                ps.setString(2, user.get_password());
                if (user instanceof Seller seller) {
                    ps.setBigDecimal(3, seller.getAccount_balance());
                } else if (user instanceof Bidder bidder) {
                    ps.setBigDecimal(3, bidder.getAccount_balance());
                } else {
                    ps.setBigDecimal(3, null);
                }
                ps.setLong(4, user.get_ID());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi update user id: " + user.get_ID(), e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return user;
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi deleteById user: " + id, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        Connection conn = null;
        try {
            conn = getConn();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByEmail: " + email, e);
        } finally {
            if (shouldClose()) closeQuietly(conn);
        }
        return Optional.empty();
    }
}