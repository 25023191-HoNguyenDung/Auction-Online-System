package com.auction.common.protocol;

import java.math.BigDecimal;

// thông tin trả về từ server sau khi login
public class LoginResPayload {
    private boolean success;
    private long userId;
    private String username;
    private String role; // quyền của user
    private BigDecimal balance; // THÊM MỚI: số dư thực tế từ DB

    // Bắt buộc phải có Constructor rỗng cho Jackson giải mã JSON
    public LoginResPayload() {
    }

    public LoginResPayload(boolean success, long userId, String username, String role) {
        this.success = success;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.balance = BigDecimal.ZERO;
    }

    // Constructor đầy đủ tham số bao gồm số dư
    public LoginResPayload(boolean success, long userId, String username, String role, BigDecimal balance) {
        this.success = success;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.balance = balance;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}