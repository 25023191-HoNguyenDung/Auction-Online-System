package com.auction.client.model;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Client AuctionItem — gộp server Auction + Item:
 *
 * Auction: id, item_id, seller_id, starting_price, current_price,
 *          status, start_time, end_time, winner_bidder_id
 * Item:    id(itemId), seller_id, name, description, category,
 *          starting_price, current_price, image_url
 */
public class AuctionItem {

    // Từ Auction
    private long          auctionId;
    private String        status;         // PENDING | RUNNING | CLOSED | CANCELLED
    private double        startingPrice;
    private double        currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long          winnerBidderId;

    // Từ Item
    private long   itemId;
    private long   sellerId;
    private String sellerName;
    private String itemName;
    private String description;
    private String category;
    private String imageUrl;

    // Tính thêm từ BidDao
    private int totalBids;

    public AuctionItem() {}

    public AuctionItem(long auctionId, long itemId, long sellerId,
                       String sellerName, String itemName, String description,
                       String category, String status,
                       double startingPrice, double currentPrice,
                       LocalDateTime startTime, LocalDateTime endTime,
                       String imageUrl, int totalBids) {
        this.auctionId     = auctionId;
        this.itemId        = itemId;
        this.sellerId      = sellerId;
        this.sellerName    = sellerName;
        this.itemName      = itemName;
        this.description   = description;
        this.category      = category;
        this.status        = status;
        this.startingPrice = startingPrice;
        this.currentPrice  = currentPrice;
        this.startTime     = startTime;
        this.endTime       = endTime;
        this.imageUrl      = imageUrl;
        this.totalBids     = totalBids;
    }

    // ── Getters ───────────────────────────────────────────────
    public long          getAuctionId()      { return auctionId; }
    public long          getItemId()         { return itemId; }
    public long          getSellerId()       { return sellerId; }
    public String        getSellerName()     { return sellerName; }
    public String        getItemName()       { return itemName; }
    public String        getDescription()    { return description; }
    public String        getCategory()       { return category; }
    public String        getStatus()         { return status; }
    public double        getStartingPrice()  { return startingPrice; }
    public double        getCurrentPrice()   { return currentPrice; }
    public LocalDateTime getStartTime()      { return startTime; }
    public LocalDateTime getEndTime()        { return endTime; }
    public String        getImageUrl()       { return imageUrl; }
    public int           getTotalBids()      { return totalBids; }
    public Long          getWinnerBidderId() { return winnerBidderId; }

    // ── Setters ───────────────────────────────────────────────
    public void setAuctionId(long v)          { this.auctionId = v; }
    public void setItemId(long v)             { this.itemId = v; }
    public void setSellerId(long v)           { this.sellerId = v; }
    public void setSellerName(String v)       { this.sellerName = v; }
    public void setItemName(String v)         { this.itemName = v; }
    public void setDescription(String v)      { this.description = v; }
    public void setCategory(String v)         { this.category = v; }
    public void setStatus(String v)           { this.status = v; }
    public void setStartingPrice(double v)    { this.startingPrice = v; }
    public void setCurrentPrice(double v)     { this.currentPrice = v; }
    public void setStartTime(LocalDateTime v) { this.startTime = v; }
    public void setEndTime(LocalDateTime v)   { this.endTime = v; }
    public void setImageUrl(String v)         { this.imageUrl = v; }
    public void setTotalBids(int v)           { this.totalBids = v; }
    public void setWinnerBidderId(Long v)     { this.winnerBidderId = v; }

    // ── Helpers ───────────────────────────────────────────────
    public int secondsLeft() {
        if (endTime == null) return 0;
        long secs = Duration.between(LocalDateTime.now(), endTime).getSeconds();
        return (int) Math.max(0, secs);
    }

    public boolean isRunning()    { return "RUNNING".equalsIgnoreCase(status); }
    public boolean isPending()    { return "PENDING".equalsIgnoreCase(status); }
    public boolean isClosed()     { return "CLOSED".equalsIgnoreCase(status); }
    public boolean isEndingSoon() { return isRunning() && secondsLeft() < 900; }

    /** Badge text dùng trong UI */
    public String getDisplayStatus() {
        if (isEndingSoon()) return "ENDING_SOON";
        if (isRunning())    return "LIVE";
        if (isPending())    return "PENDING";
        return "ENDED";
    }

    // Alias cho ViewModel tương thích
    public long   getId()         { return auctionId; }
    public String getTitle()      { return itemName; }
    public String getSubtitle()   { return description; }
    public double getCurrentBid() { return currentPrice; }
}
