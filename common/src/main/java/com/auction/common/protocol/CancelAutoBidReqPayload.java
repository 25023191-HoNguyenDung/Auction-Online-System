package com.auction.common.protocol;

public class CancelAutoBidReqPayload {
    private long userId;
    private long auctionId;

    public CancelAutoBidReqPayload() {}

    public CancelAutoBidReqPayload(long userId, long auctionId) {
        this.userId = userId;
        this.auctionId = auctionId;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }
}
