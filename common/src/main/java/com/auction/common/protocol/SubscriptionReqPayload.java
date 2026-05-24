package com.auction.common.protocol;

public class SubscriptionReqPayload {
    private long auctionId;
    public SubscriptionReqPayload() {}
    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }
}