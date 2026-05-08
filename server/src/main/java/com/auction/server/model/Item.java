package com.auction.server.model;

import java.math.BigDecimal;
import java.util.UUID;

public abstract class Item {
    protected UUID id;
    protected String name;
    protected String description;
    protected BigDecimal startingPrice;
    protected String category; // "ELECTRONICS", "ART", "VEHICLE"

    public Item(UUID id, String name, String description, BigDecimal startingPrice, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.category = category;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
