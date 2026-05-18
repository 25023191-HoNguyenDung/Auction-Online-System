
package com.auction.server.dao.jdbc;

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
// làm việc trực tiếp với db
public class JdbcUserDao implements UserDao {
    // knoi tới db
    private final DatabaseConfig db = DatabaseConfig.getInstance();
    // lấy dlieu trog db thành obj java
    private User mapRow(ResultSet rs) throws SQLException {
        // đọc từng cột ra biến Java
        long id       = rs.getLong("id");
        String username = rs.getString("user_name");
        String email    = rs.getString("email");
        String password = rs.getString("password");
        String role     = rs.getString("role");
        // phân loại user theo role
        return switch (role.toUpperCase()) {
            case "BIDDER" -> new Bidder(username, id, email, password, role, rs.getBigDecimal("account_balance"), new ArrayList<>());
            case "SELLER" -> new Seller(username, id, email, password, role, rs.getBigDecimal("account_balance"), new ArrayList<>(), new ArrayList<>());
            case "ADMIN"  -> new Admin(username, id, email, password, role);
            default -> throw new RuntimeException("Role không hợp lệ: " + role);
        };
    }

    @Override
    // tìm user với id
    public Optional<User> findById(long id) {
        // câu lệnh SQL
        String sql = "SELECT * FROM users WHERE id = ?";
        // kết nối db
        try (Connection conn = db.getConnection();
             // chuẩn bị câu SQL + chờ gắn dữ liệu vào
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            // chạy câu lệnh SQL và lấy kq trả về từ db
            ResultSet rs = ps.executeQuery();
            // nếu có dlieu trả về thì chuyển thành obj
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById user: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    // tìm user bằng name
    public Optional<User> findByUsername(String username) {
        // câu lệnh SQL
        String sql = "SELECT * FROM users WHERE user_name = ?";
        // kết nối db
        try (Connection conn = db.getConnection();
             // chuẩn bị câu SQL + chờ gắn dữ liệu vào
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery(); // chạy câu lệnh SQL và lấy kq trả về từ db
            if (rs.next()) return Optional.of(mapRow(rs)); // nếu có dlieu trả về thì chuyển thành obj
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findByUsername: " + username, e);
        }
        return Optional.empty();
    }

    @Override
    // lấy tca user trong db
    public List<User> findAll() {
        // câu lệnh SQL
        String sql = "SELECT * FROM users ORDER BY id ASC";
        List<User> list = new ArrayList<>(); // ds chứa user
        // thử kết nối
        try (Connection conn = db.getConnection();
             // chạy và lấy dlieu về
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs)); // nếu có thì add vào list
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findAll users", e);
        }
        return list;
    }

    @Override
    public User save(User user) {
        // câu lệnh SQL
        String sql = "INSERT INTO users (user_name, password, email, role) VALUES (?, ?, ?, ?)";
        // thử knoi db
        try (Connection conn = db.getConnection();
             // lấy luôn id mà db tạo
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // thay vào ?
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
            ps.executeUpdate(); // gửi câu lệnh SQL xuống db
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) user.set_ID(keys.getLong(1)); // ktra xem có id ko, có thì gắn vào obj
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save user: " + user.get_user_name(), e);
        }
        return user;
    }

    @Override
    public User update(User user) {
        // câu lệnh SQL
        String sql = "UPDATE users SET email = ?, password = ? WHERE id = ?";
        // thử knoi db
        try (Connection conn = db.getConnection();
             // tạo câu sql an toàn
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // thay vào ?
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
            ps.executeUpdate(); // gửi câu lệnh SQL xuống db
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi update user id: " + user.get_ID(), e);
        }
        return user;
    }

    @Override
    public boolean deleteById(long id) {
        // câu lệnh SQL
        String sql = "DELETE FROM users WHERE id = ?";
        // knoi tới db
        try (Connection conn = db.getConnection();
             //chuẩn bị câu SQL + chờ gắn dữ liệu vào
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0; //số dòng trong database bị thay đổi
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi deleteById user: " + id, e);
        }
    }
}
