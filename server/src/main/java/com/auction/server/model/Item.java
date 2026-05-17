package com.auction.server.model;

import java.math.BigDecimal;

public class Item {

    private long itemId;
    private long sellerId;
    private String itemName;
    private String description;
    private String category;        // ELECTRONICS | ART | VEHICLE | OTHER
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private String imageUrl;

    // Constructor mặc định
    public Item() {
    }

    // Constructor đầy đủ
    public Item(long itemId, long sellerId, String itemName, String description,
                String category, BigDecimal startingPrice, BigDecimal currentPrice,
                String imageUrl) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.itemName = itemName;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.imageUrl = imageUrl;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public long getSellerId() {
        return sellerId;
    }

    public void setSellerId(long sellerId) {
        this.sellerId = sellerId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }



    @Override
    public String toString() {
        return "Item{" +
                "itemId=" + itemId +
                ", sellerId=" + sellerId +
                ", itemName='" + itemName + '\'' +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", startingPrice=" + startingPrice +
                ", currentPrice=" + currentPrice +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}

