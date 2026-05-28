package com.auction.server.concurrency;

import java.sql.Connection;

// Giữ Connection database riêng cho từng thread bằng ThreadLocal
public class ConnectionHolder {
    // mỗi thread tự giữ Connection riêng của mình
    private static final ThreadLocal<Connection> current = new ThreadLocal<>();

    // Cất connection vào thread hiện tại
    public static void set(Connection conn) {
        current.set(conn);
    }
    // Lấy connection của thread hiện tại ra dùng
    public static Connection get() {
        return current.get();
    }


    public static void clear() {
        current.remove();
    }


    public static boolean hasConnection() {
        return current.get() != null;
    }
}