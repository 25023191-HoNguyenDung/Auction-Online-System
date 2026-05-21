package com.auction.server.model;

import java.math.BigDecimal;
import java.util.ArrayList;

public class Seller extends User {

    private BigDecimal account_balance;
    private ArrayList<Auction> history_of_auction; 
    private ArrayList<Item> sold_list_items;

    public Seller(String name, long ID, String email,
                  String password, String role,
                  BigDecimal account_balance,
                  ArrayList<Auction> history_of_auction,
                  ArrayList<Item> sold_list_items) {
        super(name, ID, email, password, role);
        this.account_balance = account_balance;
        this.history_of_auction = history_of_auction;
        this.sold_list_items = sold_list_items;
    }

    @Override
    public void set_role() {
        this.role = "SELLER";
    }

    public BigDecimal getAccount_balance() { return account_balance; }
    public void setAccount_balance(BigDecimal account_balance) {
        this.account_balance = account_balance;
    }

    public void receivePayment(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be > 0");
        this.account_balance = account_balance.add(amount);
    }

    public ArrayList<Item> getSold_list_items() { return sold_list_items; }

    public void addSoldItem(Item item) {
        if (item != null) sold_list_items.add(item);
    }

    public int getTotalSold() { return sold_list_items.size(); }

    public BigDecimal getTotalRevenue() {

        return sold_list_items.stream()
                .map(item -> item.getCurrentPrice())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

    }
    
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Withdraw amount must be > 0");
        if (amount.compareTo(account_balance) > 0)
            throw new IllegalStateException(
                "Insufficient balance — current balance is " + account_balance
            );
        this.account_balance = account_balance.subtract(amount);
    }

    public ArrayList<Auction> getHistory_of_auction() {
        return history_of_auction;
    }

    public void addAuctionHistory(Auction auction) {
        if (auction != null) history_of_auction.add(auction);
    }
}