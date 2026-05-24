package com.auction.common.protocol;

import java.util.ArrayList;
import java.util.List;

public class ListUsersResPayload {
    private List<UserSummaryItem> users = new ArrayList<>();

    public ListUsersResPayload() {}

    public ListUsersResPayload(List<UserSummaryItem> users) {
        this.users = users;
    }

    public List<UserSummaryItem> getUsers() { return users; }
    public void setUsers(List<UserSummaryItem> users) { this.users = users; }
}
