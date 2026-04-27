package com.auction.common.protocol;
//dữ liệu khi user gửi yêu cầu đăng nhập*
public class LoginReqPayload {
    private final String username;
    private final String password;

    public LoginReqPayload(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
