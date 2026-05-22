package com.auction.common.protocol;

// dữ liệu khi user gửi yêu cầu đăng nhập
public class LoginReqPayload {
    private String username;
    private String password;

    // Bắt buộc phải có Constructor rỗng cho Jackson giải mã JSON
    public LoginReqPayload() {
    }

    public LoginReqPayload(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}