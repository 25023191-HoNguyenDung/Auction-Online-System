package com.auction.server.model;
import java.time.LocalDateTime;

public class Auction {

    private String id;
    private Product product;
    private AuctionStatus status;
    private LocalDateTime startTime;    //tgian bat dau dau gia
    private LocalDateTime endTime;      //tgian ket thuc dau gia
    private double currentPrice;
    private String highestBidder;
    
    //Constructor
    public Auction( String id,
                    Product product,
                    LocalDateTime startTime,
                    LocalDateTime endTime,
                    double currentPrice,
                    String highestBidder ) {
        this.id = id;
        this.product = product;
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

    public String getProductID() { 
        return product.get_product_id(); 
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
    
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}    
