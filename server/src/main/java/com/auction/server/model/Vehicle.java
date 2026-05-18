package com.auction.server.model;

import java.math.BigDecimal;

public class Vehicle extends Item {
    
    //Fields:
    private String fuel_type;
    private String type_of_vehicle;
    private String color;
    private int year;

    //Constructor:


    public Vehicle(long itemId, long sellerId, String itemName, String description, String category, BigDecimal startingPrice, BigDecimal currentPrice, String imageUrl, String fuel_type, String type_of_vehicle, String color, int year) {
        super(itemId, sellerId, itemName, description, category, startingPrice, currentPrice, imageUrl);
        this.fuel_type = fuel_type;
        this.type_of_vehicle = type_of_vehicle;
        this.color = color;
        this.year = year;
    }

    //Getters and Setters:
    public String get_fuel_type() {
        return fuel_type;
    }
    public String get_type_of_vehicle() {
        return type_of_vehicle;
    }
    public String get_color() {
        return color;
    }
    public int get_year() {
        return year;
    }

    
    public void set_fuel_type(String fuel_type) {
        this.fuel_type = fuel_type;
    }
    public void set_type_of_vehicle(String type_of_vehicle) {
        this.type_of_vehicle = type_of_vehicle;
    }
    public void set_color(String color) {
        this.color = color;
    }
    public void set_year(int year) {
        this.year = year;
    }


}
