package com.auction.server.model;

<<<<<<< HEAD
import java.math.BigDecimal;
=======
>>>>>>> c049462482c8c94b427c0465d43fa8f4e14dd959
import java.util.ArrayList;

public class Seller extends User {

<<<<<<< HEAD
    private BigDecimal account_balance;
=======
    private double account_balance;
>>>>>>> c049462482c8c94b427c0465d43fa8f4e14dd959
    private ArrayList<Auction> history_of_auction; 
    private ArrayList<Item> sold_list_items;

    public Seller(String name, long ID, String email,
                  String password, String role,
<<<<<<< HEAD
                  BigDecimal account_balance,
=======
                  double account_balance,
>>>>>>> c049462482c8c94b427c0465d43fa8f4e14dd959
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

<<<<<<< HEAD
    public BigDecimal getAccount_balance() { return account_balance; }
    public void setAccount_balance(BigDecimal account_balance) {
        this.account_balance = account_balance;
    }

    public void receivePayment(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be > 0");
        this.account_balance = account_balance.add(amount);
=======
    public double getAccount_balance() { return account_balance; }
    public void setAccount_balance(double account_balance) {
        this.account_balance = account_balance;
    }

    public void receivePayment(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Amount must be > 0");
        this.account_balance += amount;
>>>>>>> c049462482c8c94b427c0465d43fa8f4e14dd959
    }

    public ArrayList<Item> getSold_list_items() { return sold_list_items; }

    public void addSoldItem(Item item) {
        if (item != null) sold_list_items.add(item);
    }

    public int getTotalSold() { return sold_list_items.size(); }

<<<<<<< HEAD
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
=======
    public double getTotalRevenue() {

        return sold_list_items.stream()
                .map(item -> item.getCurrentPrice().doubleValue())
                .reduce(0.0, (a, b) -> a + b);

    }
    
    public void withdraw(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Withdraw amount must be > 0");
        if (amount > account_balance)
            throw new IllegalStateException(
                "Insufficient balance — current balance is " + account_balance
            );
        this.account_balance -= amount;
>>>>>>> c049462482c8c94b427c0465d43fa8f4e14dd959
    }

    public ArrayList<Auction> getHistory_of_auction() {
        return history_of_auction;
    }

    public void addAuctionHistory(Auction auction) {
        if (auction != null) history_of_auction.add(auction);
    }
}