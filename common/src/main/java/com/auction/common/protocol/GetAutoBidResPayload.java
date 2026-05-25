package com.auction.common.protocol;

import java.math.BigDecimal;

public class GetAutoBidResPayload {
    private boolean active;
    private BigDecimal maxBid;
    private BigDecimal increment;

    public GetAutoBidResPayload() {}

    public GetAutoBidResPayload(boolean active, BigDecimal maxBid, BigDecimal increment) {
        this.active = active;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public BigDecimal getMaxBid() { return maxBid; }
    public void setMaxBid(BigDecimal maxBid) { this.maxBid = maxBid; }

    public BigDecimal getIncrement() { return increment; }
    public void setIncrement(BigDecimal increment) { this.increment = increment; }
}
