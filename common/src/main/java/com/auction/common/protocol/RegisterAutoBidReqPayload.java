package com.auction.common.protocol;

import java.math.BigDecimal;

public class RegisterAutoBidReqPayload {
    private long userId;
    private long auctionId;
    private BigDecimal maxBid;
    private BigDecimal increment;

    public RegisterAutoBidReqPayload() {}

    public RegisterAutoBidReqPayload(long userId, long auctionId, BigDecimal maxBid, BigDecimal increment) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public BigDecimal getMaxBid() { return maxBid; }
    public void setMaxBid(BigDecimal maxBid) { this.maxBid = maxBid; }

    public BigDecimal getIncrement() { return increment; }
    public void setIncrement(BigDecimal increment) { this.increment = increment; }
}
