package com.auction.common.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ListUsersResPayload {
    private final List<UserSummaryItem> users;
    private final int total;

    @JsonCreator
    public ListUsersResPayload(
            @JsonProperty("users") List<UserSummaryItem> users,
            @JsonProperty("total") int total) {
        this.users = users;
        this.total = total;
    }

    public List<UserSummaryItem> getUsers() { return users; }
    public int getTotal() { return total; }
}
