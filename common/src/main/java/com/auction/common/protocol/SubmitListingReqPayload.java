package com.auction.common.protocol;

public class SubmitListingReqPayload {
    private long sellerId;
    private String itemName;
    private String description;
    private String category;
    private double startingPrice;
    private int durationMinutes;

    public SubmitListingReqPayload() {}

    public SubmitListingReqPayload(long sellerId, String itemName, String description, String category,
                                   double startingPrice, int durationMinutes) {
        this.sellerId = sellerId;
        this.itemName = itemName;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.durationMinutes = durationMinutes;
    }

    public long getSellerId() { return sellerId; }
    public void setSellerId(long sellerId) { this.sellerId = sellerId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
}
