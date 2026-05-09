package com.auction.server.model;

public class Art extends Product {

    //Fields: 
    private String artist;
    private String style;

    
    //Constructor:
    public Art( String name,
                String description,
                double reserve_price,
                String image_url,
                String artist,
                String style) {

        super(name, description, reserve_price, image_url);
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
