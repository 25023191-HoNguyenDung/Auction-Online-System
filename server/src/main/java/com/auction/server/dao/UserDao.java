package com.auction.server.dao;

import com.auction.server.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    User save(User user);
    User update(User user);
    boolean deleteById(long id);
}
