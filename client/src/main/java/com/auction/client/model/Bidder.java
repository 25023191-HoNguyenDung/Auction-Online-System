package com.auction.client.model;
import java.util.ArrayList;


public class Bidder extends User {

    //Fields:
    double account_balance;
    ArrayList<Auction> history_of_auction;                              //nhớ tạo lớp Auction

    //Constructor:
    public Bidder(String name, String ID, String email, String password, String role, double account_balance, ArrayList<Auction> history_of_auction) {
        super(name, ID, email, password, role);
        this.account_balance = account_balance;
        this.history_of_auction = history_of_auction;
    }

    
    @Override
    public String get_role() {
        this.role = "Bidder";
        return this.role;
    }

}