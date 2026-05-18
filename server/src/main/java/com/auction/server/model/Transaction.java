package com.auction.server.model;

import java.time.LocalDateTime;

public class Transaction {
    private long id;
    private long userId;
    private String transactionType;
    private double amount;
    private Long auctionId;
    private String description;
    private LocalDateTime createdAt;

    public Transaction(long userId, String transactionType, double amount, Long auctionId, String description) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.auctionId = auctionId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public Transaction(long id, long userId, String transactionType, double amount, Long auctionId, String description, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.auctionId = auctionId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", userId=" + userId +
                ", type='" + transactionType + '\'' +
                ", amount=" + amount +
                ", auctionId=" + auctionId +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
