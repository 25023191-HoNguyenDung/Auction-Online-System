package com.auction.common.protocol;

public class AdminActionReqPayload {
    private long auctionId;
    private String action;

    public AdminActionReqPayload() {}

    public AdminActionReqPayload(long auctionId, String action) {
        this.auctionId = auctionId;
        this.action = action;
    }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
