package com.auction.client.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AuctionItem {
    private long auctionId;
    private long itemId;
    private long sellerId;
    private String sellerName;
    private String itemName;
    private String description;
    private String category;
    private String status;           // RUNNING, PENDING, CLOSED
    private double startingPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imageUrl;
    private int totalBids;

    // Constructor used in ViewModel
    public AuctionItem(long auctionId, long itemId, long sellerId, String sellerName,
                       String itemName, String description, String category, String status,
                       double startingPrice, double currentPrice,
                       LocalDateTime startTime, LocalDateTime endTime,
                       String imageUrl, int totalBids) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemName = itemName;
        this.description = description;
        this.category = category;
        this.status = status;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.imageUrl = imageUrl;
        this.totalBids = totalBids;
    }

    // Helper methods
    public boolean isRunning() { return "RUNNING".equals(status); }
    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isClosed()  { return "CLOSED".equals(status); }
    public boolean isEndingSoon() {
        return isRunning() && secondsLeft() < 900; // 15 phút
    }

    public int secondsLeft() {
        if (endTime == null) return 0;
        return (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
    }

    public String getDisplayStatus() {
        return isEndingSoon() ? "ENDING_SOON" : "LIVE";
    }

    // Getters
    public long getAuctionId() { return auctionId; }
    public long getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public double getCurrentPrice() { return currentPrice; }
    public String getSellerName() { return sellerName; }
    public String getImageUrl() { return imageUrl; }
    public int getTotalBids() { return totalBids; }
}