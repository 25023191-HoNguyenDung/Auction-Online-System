package com.auction.server.concurrency;

import java.sql.Connection;
import java.sql.SQLException;

import com.auction.server.config.DatabaseConfig;


public class TransactionManager {
    private static TransactionManager instance;
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    // dùng private tránh việc "new" tạo ra nhiều người quản lý
    private TransactionManager() {}

    // singleton
    public static synchronized TransactionManager getInstance(){
        if (instance == null) instance = new TransactionManager();
        return instance;
    }

    // mở giao dịch thủ công
    public Connection beginTransaction() throws SQLException {
        Connection con = db.getConnection();
        con.setAutoCommit(false); // Tắt autocommit -> lưu nháp các thay đổi SQL
        //Nếu không tắt autocommit, mỗi lệnh SQL chạy xong sẽ ghi thẳng vào ổ cứng DB
        return con;
    }

    // lưu thay đổi của transaction xuống db
    public void commit(Connection con){
        if (con == null) return;
        try{
            con.commit(); // đẩy dữ liệu lưu nháp xuống DB
        } catch (SQLException e){
            throw new RuntimeException("Transaction commit failed", e);
        }
    }
    
    // hủy thay đổi đã thực hiện (Xóa nháp)
    public void rollBack(Connection con) {
        if (con == null) return;
        try {
            con.rollback();
        } catch (SQLException e){
            throw new RuntimeException("Transaction rollback failed", e);
        }
        finally {
            close(con);
        }
    }

    //Đóng và giải phóng kết nối, khôi phục cài đặt gốc
    public void close(Connection con){
        try{
            if(!con.isClosed() && con != null){
                con.setAutoCommit(true);    //reset trước khi trả về pool
                con.close();
            }
        } catch (SQLException e){
            System.err.println("Transaction error: " + e.getMessage());
        }
    }


    //Cơ chế tự động hóa
    //Giao diện chức năng
    @FunctionalInterface
    public interface TransactionWork {
        void excecute(Connection con) throws Exception;
    }

    //Hàm bọc tự động - Ép toàn bộ logic nghiệp vụ phải tuân thủ đúng vòng đời chuẩn 1 Transaction an toàn
    public void executeInTransaction(TransactionWork work) {
        Connection con = null;
        try {
            con = beginTransaction();   //Mở cửa kết nối và bật lưu nháp (AutoCommit = false)
            work.excecute(con);     //Thực thi code nghiệp vụ truyền vào
            commit(con);        //Lưu dữ liệu xuống DB
        } catch (Exception e) {
            rollBack(con);  //Hủy hết dữ liệu nháp nếu có lỗi
            throw new RuntimeException("Transaction failed, all pending changes have been rolled back: " + e.getMessage(), e);
        } finally {
            close(con);     //Đảm bảo kết nối luôn được giải phóng
        }
    }







}
