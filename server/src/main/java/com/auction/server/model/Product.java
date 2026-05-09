package com.auction.server.model;

public abstract class Product {

    //Call Fields:
    private String product_name;
    private String product_id;
    private String decripsion;
    private double reserve_price;
    
    private double current_price; //Update price in auction

    private String image_url; //URL for product image
    
    
    //Constructor:
    public Product(String product_name, String product_id, String decripsion, double reserve_price, String image_url) {
        this.product_name = product_name;
        this.product_id = product_id;
        this.decripsion = decripsion;
        this.reserve_price = reserve_price;
        this.current_price = reserve_price; //Initial current price is the same as reserve price
        this.image_url = image_url;
    }

    //Getters and Setters:
    public String get_product_name() {
        return product_name;
    }

    public String get_product_id() {
        return product_id;
    }

    public String get_description() {
        return decripsion;
    }

    public double get_reserve_price() {
        return reserve_price;
    }

    public double get_current_price() {
        return current_price;
    }
    public String get_image_url() {
        return image_url;
    }


    public void set_product_name( String new_product_name ) {
        this.product_name = new_product_name;
    }

    public void set_description( String new_description ) {
        this.decripsion = new_description;
    }

    public void set_reserve_price( double new_reserve_price ) {
        this.reserve_price = new_reserve_price;
    }

    public void set_current_price( double new_current_price ) {
        this.current_price = new_current_price;
    }
    public void set_image_url( String new_image_url ) {
        this.image_url = new_image_url;
    }



}

