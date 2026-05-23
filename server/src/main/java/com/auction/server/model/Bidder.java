package com.auction.server.model;

import java.math.BigDecimal;
import java.util.ArrayList;

public class Bidder extends User {

    private BigDecimal account_balance;
    private ArrayList<Auction> history_of_auction;

    public Bidder(String name, long ID, String email,
                  String password, String role,
                  BigDecimal account_balance,
                  ArrayList<Auction> history_of_auction) {
        super(name, ID, email, password, role);
        this.account_balance = account_balance;
        this.history_of_auction = history_of_auction;
    }

    @Override
    public void set_role() {
        this.role = "BIDDER";
    }


    public BigDecimal getAccount_balance() {
        return account_balance;
    }

    public void setAccount_balance(BigDecimal account_balance) {
        this.account_balance = account_balance;
    }

    // kiểm tra đủ tiền để đặt giá
    public boolean hasEnoughBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return account_balance.compareTo(amount) >= 0;
    }

    // trừ tiền khi đặt giá
    public void deductForBid(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bid amount must be > 0");
        }
        if (account_balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance. Available: " 
                + account_balance + ", Required: " + amount);
        }
        this.account_balance = this.account_balance.subtract(amount);
    }

    // hoàn tiền khi bị outbid
    public void refundBid(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be > 0");
        }
        this.account_balance = this.account_balance.add(amount);
    }

    // trừ tiền khi thanh toán cuối cùng
    public void deductBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid deduction amount");
        }
        if (amount.compareTo(account_balance) > 0) {
            throw new IllegalStateException("Insufficient balance for final payment");
        }
        this.account_balance = account_balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be > 0");
        }
        this.account_balance = account_balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be > 0");
        }
        if (amount.compareTo(account_balance) > 0) {
            throw new IllegalStateException(
                "Cannot withdraw " + amount + " — available: " + account_balance);
        }
        this.account_balance = account_balance.subtract(amount);
    }

    public ArrayList<Auction> getHistory_of_auction() {
        return history_of_auction;
    }

    public void addAuctionHistory(Auction auction) {
        if (auction != null) {
            history_of_auction.add(auction);
        }
    }
}