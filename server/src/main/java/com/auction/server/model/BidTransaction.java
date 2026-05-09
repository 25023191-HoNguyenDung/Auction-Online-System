package com.auction.server.model;

import java.time.LocalDateTime;

public class BidTransaction {
    private final String auctionId;
    private final String bidderName;
    private final double bidAmount;
    private final LocalDateTime timeBidding;

    public BidTransaction(String auctionId, String bidderName, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.timeBidding = LocalDateTime.now();
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimeBidding() {
        return timeBidding;
    }
}
