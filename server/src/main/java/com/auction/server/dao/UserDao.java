package com.auction.server.dao;

import com.auction.server.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDao {
    Optional<User> findById(UUID userId); //tìm user theo id
    Optional<User> findByUserName(String userName);//tìm user theo name
    List<User> findAll();// lấy tất cả user
    void save(User user); // thêm user mới vào db
    void update(User user); //update thông tin user
    void delete(UUID userId); // xóa user theo id
    boolean existsByUsername(String username);// ktra trùng tên

}
