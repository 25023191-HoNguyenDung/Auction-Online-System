package com.auction.common.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterReqPayload {
    private final String username;
    private final String email;
    private final String password;
    private final String role;

    @JsonCreator
    public RegisterReqPayload(
            @JsonProperty("username") String username,
            @JsonProperty("email")    String email,
            @JsonProperty("password") String password,
            @JsonProperty("role")     String role) {
        this.username = username;
        this.email    = email;
        this.password = password;
        this.role     = role;
    }

    public String getUsername() { return username; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }
}
