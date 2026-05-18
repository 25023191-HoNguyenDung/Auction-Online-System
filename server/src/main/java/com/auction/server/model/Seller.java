package com.auction.server.model;

import java.util.ArrayList;

public class Seller extends User {

    private double account_balance;
    private ArrayList<Auction> history_of_auction; 
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
            throw new IllegalArgumentException("Amount must be > 0");
        this.account_balance += amount;
    }

    public double getAccount_balance() {
        return account_balance;
    }

    public void setAccount_balance(double account_balance) {
        this.account_balance = account_balance;
    }

}
