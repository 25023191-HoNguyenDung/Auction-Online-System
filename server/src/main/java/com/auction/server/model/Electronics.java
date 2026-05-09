package com.auction.server.model;

public class Electronics extends Product {

    //Fields:
    private String brand;
    private int warrantyMonths;
    private int year_of_manufacture;

    //Constructor:
    public Electronics( String product_name, 
                        String description, 
                        double reservePrice,
                        String image_url, 
                        String brand, 
                        int warrantyMonths,
                        int year_of_manufacture) {       
        super(product_name, description, reservePrice, image_url);
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
