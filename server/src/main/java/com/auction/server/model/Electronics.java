package com.auction.server.model;

public class Electronics extends Product {

    //Fields:
    private String brand;
    private int warrantyMonths;

    //Constructor:
    public Electronics(String name, String description, double reservePrice,
                       String imageUrl, String brand, int warrantyMonths) {
        super(name, description, reservePrice, imageUrl);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }
    
    //getters and setters:
    public String getBrand() { 
        return brand; 
    }
    public int getWarrantyMonths() { 
        return warrantyMonths; 
    }
    public void setBrand(String brand) { 
        this.brand = brand; 
    }
    public void setWarrantyMonths(int warrantyMonths) { 
        this.warrantyMonths = warrantyMonths; 
    }
}
