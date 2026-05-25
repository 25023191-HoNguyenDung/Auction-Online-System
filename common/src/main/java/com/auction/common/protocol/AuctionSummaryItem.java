package com.auction.common.protocol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AuctionSummaryItem {
    private long auctionId;
    private String itemName;
    private String description;
    private String category;
    private BigDecimal currentHighestBid;
    private String status; 
    private Instant endTime;
    private List<String> bidHistory = new ArrayList<>();
    private Instant startTime;
    private long sellerId;
    private String sellerName;
    private BigDecimal startingPrice;

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

    public AuctionSummaryItem(long auctionId, String itemName, String description, String category, 
                              BigDecimal currentHighestBid, String status, Instant endTime, List<String> bidHistory) {
        this(auctionId, itemName, description, category, currentHighestBid, status, endTime);
        this.bidHistory = bidHistory;
    }

    public AuctionSummaryItem(long auctionId, String itemName, String description, String category, 
                              BigDecimal currentHighestBid, String status, Instant endTime, List<String> bidHistory,
                              long sellerId, String sellerName) {
        this(auctionId, itemName, description, category, currentHighestBid, status, endTime, bidHistory);
        this.sellerId = sellerId;
        this.sellerName = sellerName;
    }

    public AuctionSummaryItem(long auctionId, String itemName, String description, String category, 
                              BigDecimal currentHighestBid, String status, Instant endTime, List<String> bidHistory,
                              long sellerId, String sellerName, Instant startTime, BigDecimal startingPrice) {
        this(auctionId, itemName, description, category, currentHighestBid, status, endTime, bidHistory, sellerId, sellerName);
        this.startTime = startTime;
        this.startingPrice = startingPrice;
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

    public List<String> getBidHistory() { return bidHistory; }
    public void setBidHistory(List<String> bidHistory) { this.bidHistory = bidHistory; }

    public long getSellerId() { return sellerId; }
    public void setSellerId(long sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

}