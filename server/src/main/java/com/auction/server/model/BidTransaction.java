package com.auction.server.model;

import java.time.LocalDateTime;

public class BidTransaction {
    private static int bidId;
    private String auctionId;
    private Bidder bidder;
    private double bidAmount;
    private LocalDateTime timeBidding;

    public BidTransaction(int bidId, String auctionId, Bidder bidder, double bidAmount) {
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timeBidding = LocalDateTime.now();
        bidId++;
    }

    public int getBidId() {
        return bidId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderName() {
        return bidder.get_user_name();
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimeBidding() {
        return timeBidding;
    }
}
