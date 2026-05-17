package com.auction.server.concurrency;

import com.auction.server.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.SQLException;

import static org.postgresql.util.JdbcBlackHole.close;

public class TransactionManager {
    private static TransactionManager instance;
    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public TransactionManager() {
    }
    // singleton
    public static synchronized TransactionManager getInstance(){
        if (instance==null) instance = new TransactionManager();
        return instance;
    }
    // knoi db và đb transaction
    public Connection beginTransaction() throws SQLException {
        Connection con = db.getConnection();
        con.setAutoCommit(false); // các thay đổi SQL chưa được lưu ngay
        return con;
    }
    // lưu thay đổi của transaction xuống db
    public void commit(Connection con){
        if(con==null) return;
        try{
            con.commit(); // ghi xuống db
        }catch (SQLException e){
            throw new RuntimeException("Transaction commit failed", e);
        }finally {
            close(con);
        }
    }
    // hủy thay đổi đã thực hiện
    public void rollBack(Connection con){
        if(con==null)return;
        try{
            con.rollback();
        }catch (SQLException e){
            throw new RuntimeException("Transaction rollback failed", e);
        }
        finally {
            close(con);
        }
    }
    private void close(Connection con){
        try{
            if(!con.isClosed()){
                con.setAutoCommit(true);
                con.close();
            }
        }catch (SQLException e){}
    }







}
