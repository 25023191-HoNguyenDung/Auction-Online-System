package com.auction.server.dao.jdbc;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.ItemDao;
import com.auction.server.model.Item;
// làm việc trực tiếp với db
public class JdbcItemDao implements ItemDao {
    // knoi tới db
    private final DatabaseConfig db = DatabaseConfig.getInstance();
    // biến dlieu trong db thành obj trong java
    private Item mapRow(ResultSet rs) throws SQLException {
        Item item = new Item();
        // lấy các cột gắn vào thuộc tính của obj
        item.setItemId(rs.getLong("id"));
        item.setSellerId(rs.getLong("seller_id"));
        item.setItemName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setCategory(rs.getString("category"));
        item.setStartingPrice(rs.getBigDecimal("starting_price"));
        item.setCurrentPrice(rs.getBigDecimal("current_price"));
        item.setImageUrl(rs.getString("image_url"));
        return item;
    }

    @Override // tìm theo id
    public Optional<Item> findById(long id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = db.getConnection();
             // chuẩn bị câu SQL + chờ gắn dữ liệu vào
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id); // gắn gtri vào dấu ?
            ResultSet rs = ps.executeQuery(); // chạy SQL và lấy kq trả về từ db
            if (rs.next()) return Optional.of(mapRow(rs)); // nếu có dữ liệu thì chuyển về obj item
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findById item: " + id, e);
        }
        return Optional.empty();
    }

    @Override // lấy tất cả item của 1 người bán
    public List<Item> findBySellerId(long sellerId) {
        String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY id DESC"; // câu lệnh SQL
        List<Item> list = new ArrayList<>(); // ds để vứt item vào
        // thử knoi
        try (Connection conn = db.getConnection();
             // chuẩn bị câu SQL + chờ gắn dữ liệu vào
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sellerId); // gắn gtri vào dấu ?
            ResultSet rs = ps.executeQuery(); // chạy và lấy dlieu từ db về
            while (rs.next()) list.add(mapRow(rs)); // nếu dlieu có thì thêm vào list
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findBySellerId: " + sellerId, e);
        }
        return list;
    }

    @Override
    // lấy tất cả item của hệ thống
    public List<Item> findAll() {
        String sql = "SELECT * FROM items ORDER BY id DESC"; // câu lệnh SQL
        List<Item> list = new ArrayList<>(); // ds đựng item
        // knoi tới db
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement(); // ko có biến truyền vào lên dùng Statement th
             // chạy và lấy dlieu về
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs)); // nếu có thì add vào list
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi findAll items", e);
        }
        return list;
    }

    @Override
    // thêm 1 item vào db
    public Item save(Item item) {
        String sql = """
            INSERT INTO items (seller_id, name, description, category, starting_price, current_price, image_url)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """; // câu lệnh SQL
        // kết nối db
        try (Connection conn = db.getConnection();
             // lấy luôn id mà db tạo
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // thay vào ?
            ps.setLong(1, item.getSellerId());
            ps.setString(2, item.getItemName());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getCategory());
            ps.setBigDecimal(5, item.getStartingPrice());
            ps.setBigDecimal(6, item.getCurrentPrice());
            ps.setString(7, item.getImageUrl());
            ps.executeUpdate(); // gửi câu lệnh SQL xuống db
            ResultSet keys = ps.getGeneratedKeys(); // lấy id tự sinh
            if (keys.next()) item.setItemId(keys.getLong(1)); // ktra xem có id ko, có thì gắn vào obj
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi save item: " + item.getItemName(), e);
        }
        return item;
    }

    @Override
    public Item update(Item item) {
        // câu lệnh SQl
        String sql = """
            UPDATE items
            SET name = ?, description = ?, category = ?,
                starting_price = ?, current_price = ?, image_url = ?
            WHERE id = ?
        """;
        // thử kết nối
        try (Connection conn = db.getConnection();
             // tạo câu sql an toàn
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // thay vào ?
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getCategory());
            ps.setBigDecimal(4, item.getStartingPrice());
            ps.setBigDecimal(5, item.getCurrentPrice());
            ps.setString(6, item.getImageUrl());
            ps.setLong(7, item.getItemId());
            ps.executeUpdate();// gửi câu lệnh SQL xuống db
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi update item id: " + item.getItemId(), e);
        }
        return item;
    }

    @Override
    public boolean deleteById(long id) {
        String sql = "DELETE FROM items WHERE id = ?"; // câu lệnh SQL
        // knoi tới db
        try (Connection conn = db.getConnection();
             //chuẩn bị câu SQL + chờ gắn dữ liệu vào
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0; //số dòng trong database bị thay đổi
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi deleteById item: " + id, e);
        }
    }
}