package com.auction.server.dao.jdbc;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.dao.UserDao;
import com.auction.server.model.User;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcUserDao implements UserDao {
    private Connection getConn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }
    @Override
    public Optional<User> findById(UUID userId) {

        return Optional.empty();
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        return List.of();
    }

    @Override
    public void save(User user) {
    }

    @Override
    public void update(User user) {

    }

    @Override
    public void delete(UUID userId) {

    }

    @Override
    public boolean existsByUsername(String username) {
        return false;
    }
}
