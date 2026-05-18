package com.auction.server.model;
import java.util.ArrayList;


public class Bidder extends User {

    //Fields:
    double account_balance;
    ArrayList<Auction> history_of_auction;                              //nhớ tạo lớp Auction

    //Constructor:
    public Bidder(  String name,
                    long ID,
                    String email, 
                    String password, 
                    String role, 
                    double account_balance, 
                    ArrayList<Auction> history_of_auction   ) {

        super(name, ID, email, password, role);
        this.account_balance = account_balance;
        this.history_of_auction = history_of_auction;
        
    }

    
    @Override
    public void set_role() {
        this.role = "Bidder";
    }

    // Getters and Setters:
    public double getAccount_balance() {
        return account_balance;
    }   
    public void setAccount_balance(double account_balance) {
        this.account_balance = account_balance;
    }
    public ArrayList<Auction> getHistory_of_auction() {
        return history_of_auction;
    }

    


}