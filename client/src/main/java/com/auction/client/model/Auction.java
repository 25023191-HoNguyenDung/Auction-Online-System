package com.auction.client.model;
import java.time.LocalDateTime;

public class Auction {

    private String id;
    private String productName;
    private AuctionStatus status;
    private LocalDateTime startTime;    //tgian bat dau dau gia
    private LocalDateTime endTime;      //tgian ket thuc dau gia
    private double currentPrice;
    private String highestBidder;
    
    //Constructor
    public Auction(String id, String productName, LocalDateTime startTime, LocalDateTime endTime, double currentPrice, String highestBidder) {
        this.id = id;
        this.productName = productName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.highestBidder = highestBidder;
        this.status = AuctionStatus.OPEN;
    }

    //Getters and Setters
    public String getId() { 
        return id; 
    }   

    public String getProductName() { 
        return productName; 
    }
    
    public AuctionStatus getStatus() { 
        return status; 
    }

    public LocalDateTime getStartTime() { 
        return startTime; 
    }

    public LocalDateTime getEndTime() { 
        return endTime; 
    }

    public double getCurrentPrice() { 
        return currentPrice; 
    }

    public String getHighestBidder() { 
        return highestBidder; 
    }


    public void setCurrentPrice(double currentPrice) { 
        this.currentPrice = currentPrice; 
    }

    public void setHighestBidder(String highestBidder) { 
        this.highestBidder = highestBidder; 
    }
    
}    
