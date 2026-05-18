
package com.auction.server.dao.jdbc;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.UserDao;
import com.auction.server.model.Admin;
import com.auction.server.model.Bidder;
import com.auction.server.model.Seller;
import com.auction.server.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserDao implements UserDao {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private User mapRow(ResultSet rs) throws SQLException {
        long id       = rs.getLong("id");
        String username = rs.getString("user_name");
        String email    = rs.getString("email");
        String password = rs.getString("password");
        String role     = rs.getString("role");
        double balance  = rs.getDouble("account_balance");

        User user = switch (role.toUpperCase()) {
            case "BIDDER" -> new Bidder(username, id, email, password, role, balance, new ArrayList<>());
            case "SELLER" -> new Seller(username, id, email, password, role, balance, new ArrayList<>(), new ArrayList<>());
            case "ADMIN"  -> new Admin(username, id, email, password, role);
            default -> throw new RuntimeException("Role không hợp lệ: " + role);
        };
        return user;
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById user: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE user_name = ?";  // user_name
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByUsername: " + username, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id ASC";
        List<User> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findAll users", e);
        }
        return list;
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (user_name, password, email, role, account_balance) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.get_user_name());
            ps.setString(2, user.get_password());
            ps.setString(3, user.get_email());
            ps.setString(4, user.getRole());
            double balance = (user instanceof Bidder) ? ((Bidder) user).getAccount_balance()
                           : (user instanceof Seller) ? ((Seller) user).getAccount_balance()
                           : 0.0;
            ps.setDouble(5, balance);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.set_ID(keys.getLong(1));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save user: " + user.get_user_name(), e);
        }
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, password = ?, account_balance = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.get_email());
            ps.setString(2, user.get_password());
            double balance = (user instanceof Bidder) ? ((Bidder) user).getAccount_balance()
                           : (user instanceof Seller) ? ((Seller) user).getAccount_balance()
                           : 0.0;
            ps.setDouble(3, balance);
            ps.setLong(4, user.get_ID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi update user id: " + user.get_ID(), e);
        }
        return user;
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi deleteById user: " + id, e);
        }
    }

    public double getBalance(long userId) {
        String sql = "SELECT account_balance FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("account_balance");
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi getBalance user: " + userId, e);
        }
        return 0.0;
    }

    public void updateBalance(long userId, double newBalance) {
        String sql = "UPDATE users SET account_balance = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi updateBalance user: " + userId, e);
        }
    }
}
