package com.auction.common.protocol;

import java.math.BigDecimal;
import java.time.Instant;
// hiển thị của phiên đấu giá
public class AuctionSummaryItem {
    private final long auctionId;
    private final String itemName;
    private final BigDecimal currentHighestBid;
    private final String status; // trạng thái phiên đấu giá
    private final Instant endTime;

    public AuctionSummaryItem(long auctionId, String itemName, BigDecimal currentHighestBid, String status, Instant endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentHighestBid = currentHighestBid;
        this.status = status;
        this.endTime = endTime;
    }

    public long getAuctionId() {
        return auctionId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public String getStatus() {
        return status;
    }

    public Instant getEndTime() {
        return endTime;
    }
}
