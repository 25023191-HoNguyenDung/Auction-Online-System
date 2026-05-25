package com.auction.client.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AuctionItem {
    private long auctionId;
    private long itemId;
    private long sellerId;
    private String sellerName;
    private String itemName;
    private String description;
    private String category;
    private String status;           // RUNNING, PENDING, CLOSED, FINISHED, PAID, CANCELLED
    private double startingPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imageUrl;
    private int totalBids;
    private List<String> bidHistory = new ArrayList<>();

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

    public AuctionItem(long auctionId, long itemId, long sellerId, String sellerName,
                       String itemName, String description, String category, String status,
                       double startingPrice, double currentPrice,
                       LocalDateTime startTime, LocalDateTime endTime,
                       String imageUrl, int totalBids, List<String> bidHistory) {
        this(auctionId, itemId, sellerId, sellerName, itemName, description, category, status,
             startingPrice, currentPrice, startTime, endTime, imageUrl, totalBids);
        this.bidHistory = bidHistory;
    }

    // ── Status helpers ────────────────────────────────────────
    // FIX: Kiểm tra status terminal TRƯỚC, không phụ thuộc vào timer
    private static final java.util.Set<String> TERMINAL_STATUSES =
        java.util.Set.of("CLOSED", "FINISHED", "PAID", "CANCELLED");

    public boolean isClosed() {
        // Nếu status là terminal (PAID, FINISHED, CANCELLED, CLOSED) → luôn closed
        if (status != null && TERMINAL_STATUSES.contains(status.toUpperCase())) return true;
        // Nếu không phải pending và hết giờ → cũng closed
        return !isPending() && secondsLeft() <= 0;
    }

    public boolean isPending() {
        return "PENDING".equals(status) || "OPEN".equals(status);
    }

    public boolean isRunning() {
        if (isClosed()) return false;
        if (isPending()) return false;
        return secondsLeft() > 0;
    }

    public boolean isEndingSoon() {
        return isRunning() && secondsLeft() <= 300; // 5 phút
    }

    public int secondsLeft() {
        if (endTime == null) return 0;
        return (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
    }

    public String getDisplayStatus() {
        if (isClosed()) return "CLOSED";
        if (isPending()) return "UPCOMING";
        if (isEndingSoon()) return "ENDING_SOON";
        return "LIVE";
    }

    // ── Getters ───────────────────────────────────────────────
    public long getAuctionId() { return auctionId; }
    public long getItemId() { return itemId; }
    public long getSellerId() { return sellerId; }
    public String getItemName() { return itemName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public String getSellerName() { return sellerName; }
    public String getImageUrl() { return imageUrl; }
    public int getTotalBids() { return totalBids; }
    public List<String> getBidHistory() { return bidHistory; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    // ── Setters ───────────────────────────────────────────────
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setTotalBids(int totalBids) { this.totalBids = totalBids; }
    public void setStatus(String status) { this.status = status; }
    public void setBidHistory(List<String> bidHistory) { this.bidHistory = bidHistory; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
