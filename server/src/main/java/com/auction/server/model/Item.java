package com.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class Item {
    private long id;
    private long seller_Id;
    private String name;
    private String description;
    private String category;       // ELECTRONICS | ART | VEHICLE | OTHER
    private BigDecimal starting_price;
    private LocalDateTime created_at;

    public Item() {}

    public Item(long id, long seller_Id, String name, String description,
                String category, BigDecimal starting_price, LocalDateTime created_at) {
        this.id = id;
        this.seller_Id = seller_Id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.starting_price = starting_price;
        this.created_at = created_at;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSellerId() { return seller_Id; }
    public void setSellerId(long sellerId) { this.seller_Id = seller_Id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getStartingPrice() { return starting_price; }
    public void setStartingPrice(BigDecimal startingPrice) { this.starting_price = starting_price; }

    public LocalDateTime getCreatedAt() { return created_at; }
    public void setCreatedAt(LocalDateTime createdAt) { this.created_at = createdAt; }

    @Override
    public String toString() {
        return "Item{id=" + id + ", name='" + name + "', category='" + category + "'}";
    }
}
