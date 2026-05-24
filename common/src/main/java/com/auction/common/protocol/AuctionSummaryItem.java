package com.auction.common.protocol;

import java.math.BigDecimal;
import java.time.Instant;

public class AuctionSummaryItem {
    private long auctionId;
    private String itemName;
    private String description;
    private String category;
    private BigDecimal currentHighestBid;
    private String status; 
    private Instant endTime;

    // Constructor rỗng cho Jackson giải mã JSON
    public AuctionSummaryItem() {
    }

    public AuctionSummaryItem(long auctionId, String itemName, String description, String category, 
                              BigDecimal currentHighestBid, String status, Instant endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.description = description;
        this.category = category;
        this.currentHighestBid = currentHighestBid;
        this.status = status;
        this.endTime = endTime;
    }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(BigDecimal currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}