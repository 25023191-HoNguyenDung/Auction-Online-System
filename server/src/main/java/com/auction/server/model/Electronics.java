package com.auction.server.model;

import java.math.BigDecimal;

public class Electronics extends Item {

    //Fields:
    private String brand;
    private int warrantyMonths;
    private int year_of_manufacture;

    //Constructor:


    public Electronics(long itemId, long sellerId, String itemName, String description, String category, BigDecimal startingPrice, BigDecimal currentPrice, String imageUrl, double reserve_price, String brand, int warrantyMonths, int year_of_manufacture) {
        super(itemId, sellerId, itemName, description, category, startingPrice, currentPrice, imageUrl, reserve_price);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.year_of_manufacture = year_of_manufacture;
    }

    //getters and setters:
    public String getBrand() { 
        return brand; 
    }
    public int getWarrantyMonths() { 
        return warrantyMonths; 
    }
    public int getYearOfManufacture() { 
        return year_of_manufacture; 
    }
    public void setBrand(String brand) { 
        this.brand = brand; 
    }
    public void setWarrantyMonths(int warrantyMonths) { 
        this.warrantyMonths = warrantyMonths; 
    }
}
