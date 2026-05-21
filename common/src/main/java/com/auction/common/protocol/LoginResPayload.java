package com.auction.common.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;//thông tin trả về từ server sau khi login
public class LoginResPayload {
    private final boolean success;
    private final long userId;
    private final String username;
    private final String role;

    @JsonCreator
    public LoginResPayload(
            @JsonProperty("success")  boolean success,
            @JsonProperty("userId")   long userId,
            @JsonProperty("username") String username,
            @JsonProperty("role")     String role) {
        this.success = success;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public boolean isSuccess() { return success; }
    public long getUserId()    { return userId; }
    public String getUsername(){ return username; }
    public String getRole()    { return role; }
}
