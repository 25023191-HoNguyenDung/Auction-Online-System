package com.auction.client.model;

import java.time.LocalDateTime;

public class Transaction {
    private long id;
    private long userId;
    private String type;
    private double amount;
    private Long auctionId;
    private String description;
    private LocalDateTime timestamp;

    public Transaction(long id, long userId, String type, double amount, Long auctionId, String description, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
        this.description = description;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public Long getAuctionId() { return auctionId; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return type + ": $" + amount + " - " + description;
    }
}
