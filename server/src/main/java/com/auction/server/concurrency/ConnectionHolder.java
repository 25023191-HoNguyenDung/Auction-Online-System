package com.auction.server.concurrency;

import java.sql.Connection;


public class ConnectionHolder {

    private static final ThreadLocal<Connection> current = new ThreadLocal<>();

    public static void set(Connection conn) {
        current.set(conn);
    }


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