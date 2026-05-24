package com.auction.server.concurrency;

import java.sql.Connection;
import java.sql.SQLException;


import com.auction.server.config.DatabaseConfig;

/**
 * Quản lý transaction — phiên bản đã sửa để dùng ConnectionHolder (ThreadLocal).
 * Giờ đây commit/rollback mới thực sự bao gồm TẤT CẢ lệnh SQL trong cùng thread.
 *
 * PATH: server/src/main/java/com/auction/server/concurrency/TransactionManager.java
 */
public class TransactionManager {
    private static TransactionManager instance;
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    private TransactionManager() {}

    public static synchronized TransactionManager getInstance() {
        if (instance == null) instance = new TransactionManager();
        return instance;
    }

    public Connection beginTransaction() throws SQLException {
        Connection con = db.getConnection();
        con.setAutoCommit(false);
        return con;
    }

    public void commit(Connection con) {
        if (con == null) return;
        try {
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Transaction commit failed", e);
        }
    }

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

    @FunctionalInterface
    public interface TransactionWork {
        void excecute(Connection con) throws Exception;
    }

    /**
     * PHIÊN BẢN ĐÃ SỬA:
     * - Gọi ConnectionHolder.set(con) trước khi chạy work
     * - Gọi ConnectionHolder.clear() trong finally
     * → Các DAO sẽ tự động dùng chung connection này thay vì tự mở connection mới.
     */
    public void executeInTransaction(TransactionWork work) {
        Connection con = null;
        try {
            con = beginTransaction();
            ConnectionHolder.set(con);      // ← THÊM MỚI: đăng ký connection vào ThreadLocal
            work.excecute(con);
            commit(con);
        } catch (Exception e) {
            rollBack(con);
            throw new RuntimeException("Transaction failed, all pending changes have been rolled back: " + e.getMessage(), e);
        } finally {
            ConnectionHolder.clear();       // ← THÊM MỚI: dọn dẹp ThreadLocal
            close(con);
        }
    }
}