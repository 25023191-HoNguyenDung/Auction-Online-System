package com.auction.common.protocol;
//thông tin trả về từ server sau khi login
public class LoginResPayload {
    private final boolean success;
    private final String userId;
    private final String username;
    private final String role; // quyền của user

    public LoginResPayload(boolean success, String userId, String username, String role) {
        this.success = success;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
