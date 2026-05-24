package com.auction.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


// qly knoi tới db
public class DatabaseConfig {

    private static volatile DatabaseConfig instance; // Singleton
    private final HikariDataSource dataSource; // pool chứa các connection ss dùng

    private DatabaseConfig() {
        HikariConfig config = new HikariConfig(); // cho Hikari biết cách kết nối database

        String url = System.getenv("DB_URL");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        // Sửa 26.134.195.52 thành localhost
        config.setJdbcUrl("jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8");
        config.setUsername("root");
        config.setPassword("Pach2308@");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("AuctionPool");
        // tạo nhóm connection
        this.dataSource = new HikariDataSource(config);
    }


    public static DatabaseConfig getInstance() {
        if (instance == null) {
            synchronized (DatabaseConfig.class) {
                if (instance == null) {
                    instance = new DatabaseConfig();
                }
            }
        }
        return instance;
    }


    public DataSource getDataSource() {
        return dataSource;
    }


    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }


    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

}
