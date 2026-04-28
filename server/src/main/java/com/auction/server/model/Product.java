package com.auction.server.model;
import java.time.LocalDateTime;

public abstract class Product {

    //Call Fields:
    String product_name;
    String decripsion;
    double reserve_price;
    
    double current_price; //Update price in auction

    LocalDateTime start_time;
    LocalDateTime end_time;
    
    
    //Constructor:
    public Product(String product_name, String decripsion, double reserve_price, double current_price ) {
        this.product_name = product_name;
        this.decripsion = decripsion;
        this.reserve_price = reserve_price;
        this.current_price = current_price;
    }





}

