package com.auction.server.model;
import java.util.ArrayList;

public class Seller extends User {

    //Fields
    double account_balance;
    ArrayList<Item> sold_list_items;

    //Constructor:
    public Seller(  String name,
                    long ID,
                    String email, 
                    String password, 
                    String role, 
                    double account_balance, 
                    ArrayList<Auction> history_of_auction, 
                    ArrayList<Item> sold_list_items) {
        
        super(name, ID, email, password, role);
        this.account_balance = account_balance;
        this.sold_list_items = sold_list_items;

    }
    
    @Override
    public void set_role() {
        this.role = "Seller";
    }

    public double getAccount_balance() {
        return account_balance;
    }

    public void setAccount_balance(double account_balance) {
        this.account_balance = account_balance;
    }

}
