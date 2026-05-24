package com.auction.common.protocol;

public class UpdateUserReqPayload {
    private long userId;
    private String newRole;

    public UpdateUserReqPayload() {}

    public UpdateUserReqPayload(long userId, String newRole) {
        this.userId = userId;
        this.newRole = newRole;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getNewRole() { return newRole; }
    public void setNewRole(String newRole) { this.newRole = newRole; }
}
