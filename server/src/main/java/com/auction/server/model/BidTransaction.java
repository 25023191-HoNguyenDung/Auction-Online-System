package com.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransaction {

    private long id;
    private long auctionId;
    private Bidder bidder;
    private long bidderId;
    private BigDecimal bidAmount;
    private LocalDateTime timeBidding;


    public BidTransaction(long auctionId, Bidder bidder, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.bidderId = bidder.get_ID();
        this.bidAmount = bidAmount;
        this.timeBidding = LocalDateTime.now();
    }


    public BidTransaction(long id, long auctionId, Bidder bidder, BigDecimal bidAmount, LocalDateTime timeBidding) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.bidderId = bidder.get_ID();
        this.bidAmount = bidAmount;
        this.timeBidding = timeBidding;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public Bidder getBidder() { return bidder; }
    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
        this.bidderId = bidder.get_ID();
    }

    public long getBidderId() { return bidderId; }

    public BigDecimal getBidAmount() { return bidAmount; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getTimeBidding() { return timeBidding; }
    public void setTimeBidding(LocalDateTime timeBidding) { this.timeBidding = timeBidding; }

    // Giữ lại để không break code cũ dùng getBidderName()
    public String getBidderName() { return bidder.get_user_name(); }

    @Override
    public String toString() {
        return "BidTransaction{id=" + id + ", auctionId=" + auctionId
                + ", bidderId=" + bidderId + ", amount=" + bidAmount + "}";
    }
}