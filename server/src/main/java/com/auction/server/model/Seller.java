package com.auction.server.model;

import java.util.ArrayList;

public class Seller extends User {

    private double account_balance;
    private ArrayList<Auction> history_of_auction; // thêm vào: Seller cũng cần
    private ArrayList<Item> sold_list_items;

    public Seller(String name, long ID, String email,
                  String password, String role,
                  double account_balance,
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

    public double getAccount_balance() { return account_balance; }
    public void setAccount_balance(double account_balance) {
        this.account_balance = account_balance;
    }

    public void receivePayment(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Số tiền nhận phải > 0");
        this.account_balance += amount;
    }

    public ArrayList<Item> getSold_list_items() { return sold_list_items; }

    public void addSoldItem(Item item) {
        if (item != null) sold_list_items.add(item);
    }

    public int getTotalSold() { return sold_list_items.size(); }

    public double getTotalRevenue() {

        return sold_list_items.stream()
                .map(item -> item.getCurrentPrice().doubleValue())
                .reduce(0.0, (a, b) -> a + b);
    }

    public ArrayList<Auction> getHistory_of_auction() {
        return history_of_auction;
    }

    public void addAuctionHistory(Auction auction) {
        if (auction != null) history_of_auction.add(auction);
    }
}