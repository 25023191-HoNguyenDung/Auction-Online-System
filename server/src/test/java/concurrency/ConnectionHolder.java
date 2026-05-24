package concurrency;

import java.sql.Connection;

/**
 * Lưu Connection của transaction hiện tại theo từng thread (ThreadLocal).
 * Giúp các DAO dùng chung 1 connection khi đang trong transaction,
 * thay vì mỗi DAO tự mở connection mới → commit/rollback mới có hiệu lực.
 *
 * PATH: server/src/main/java/com/auction/server/concurrency/ConnectionHolder.java
 */
public class ConnectionHolder {

    private static final ThreadLocal<Connection> current = new ThreadLocal<>();

    /** TransactionManager gọi khi mở transaction */
    public static void set(Connection conn) {
        current.set(conn);
    }

    /**
     * DAO gọi để lấy connection hiện tại.
     * Trả về null nếu không có transaction đang chạy.
     */
    public static Connection get() {
        return current.get();
    }

    /** TransactionManager gọi trong finally để dọn dẹp */
    public static void clear() {
        current.remove();
    }

    /** Kiểm tra có đang trong transaction không */
    public static boolean hasConnection() {
        return current.get() != null;
    }
}