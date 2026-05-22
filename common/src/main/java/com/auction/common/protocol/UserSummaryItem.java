package com.auction.common.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserSummaryItem {
    private final long id;
    private final String username;
    private final String email;
    private final String role;

    @JsonCreator
    public UserSummaryItem(
            @JsonProperty("id")       long id,
            @JsonProperty("username") String username,
            @JsonProperty("email")    String email,
            @JsonProperty("role")     String role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public long getId()       { return id; }
    public String getUsername(){ return username; }
    public String getEmail()  { return email; }
    public String getRole()   { return role; }
}
