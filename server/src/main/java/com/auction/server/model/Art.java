package com.auction.server.model;

import java.math.BigDecimal;

public class Art extends Item {

    //Fields: 
    private String artist;
    private String style;

    
    //Constructor:


    public Art(long itemId, long sellerId, String itemName, String description, String category, BigDecimal startingPrice, BigDecimal currentPrice, String imageUrl, String artist, String style) {
        super(itemId, sellerId, itemName, description, category, startingPrice, currentPrice, imageUrl);
        this.artist = artist;
        this.style = style;
    }

    //getters and setters:
    public String getArtist() { 
        return artist; 
    }
    public String getStyle() { 
        return style; 
    }
    public void setArtist(String artist) { 
        this.artist = artist; 
    }
    public void setStyle(String style) { 
        this.style = style; 
    }
    


}
