package com.auction.server.concurrency;

import java.sql.Connection;
import java.sql.SQLException;


import com.auction.server.config.DatabaseConfig;

// quản lý transaction trong database
public class TransactionManager {
    private static TransactionManager instance;
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private TransactionManager() {}
    // chỉ có 1 TransactionManager
    public static synchronized TransactionManager getInstance() {
        if (instance == null) instance = new TransactionManager();
        return instance;
    }
    // ko tự lưu bid ngay mà chờ commit/rollback
    public Connection beginTransaction() throws SQLException {
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        return con;
    }
    // lưu toàn bộ thay đổi xuống DB
    public void commit(Connection con) {
        if (con == null) return;
        try {
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Transaction commit failed", e);
        }
    }
    // huỷ tất cả thay đổi chưa commit
    public void rollBack(Connection con) {
        if (con == null) return;
        try {
            con.rollback();
        } catch (SQLException e) {
            throw new RuntimeException("Transaction rollback failed", e);
        } finally {
            close(con);
        }
    }
    // Đóng kết nối DB
    public void close(Connection con) {
        try {
            if (con != null && !con.isClosed()) {
                con.setAutoCommit(true);
                con.close();
            }
        } catch (SQLException e) {
            System.err.println("Transaction close error: " + e.getMessage());
        }
    }
    // truyền logic transaction vào
    @FunctionalInterface
    public interface TransactionWork {
        void excecute(Connection con) throws Exception;
    }
    // chạy
    public void executeInTransaction(TransactionWork work) {
        Connection con = null;
        try {
            con = beginTransaction();
            ConnectionHolder.set(con);
            work.excecute(con);
            commit(con);
        } catch (Exception e) {
            rollBack(con);
            throw new RuntimeException("Transaction failed, all pending changes have been rolled back: " + e.getMessage(), e);
        } finally {
            ConnectionHolder.clear();
            close(con);
        }
    }
}