package com.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoBidProfile {

    private long id;
    private long user_id;
    private long auction_id;
    private BigDecimal max_bid;
    private BigDecimal increment;
    private LocalDateTime created_at;
    private boolean active;

    public AutoBidProfile() {}

    public AutoBidProfile(long id, long user_id, long auction_id,
                          BigDecimal max_bid, BigDecimal increment,
                          LocalDateTime created_at) {
        this.id = id;
        this.user_id = user_id;
        this.auction_id = auction_id;
        this.max_bid = max_bid;
        this.increment = increment;
        this.created_at = created_at;
        this.active = true; // Mặc định khi tạo mới sẽ active   
    }



    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUser_id() { return user_id; }
    public void setUser_id(long user_id) { this.user_id = user_id; }

    public long getAuction_id() { return auction_id; }
    public void setAuction_id(long auction_id) { this.auction_id = auction_id; }

    public BigDecimal getMax_bid() { return max_bid; }
    public void setMax_bid(BigDecimal max_bid) { this.max_bid = max_bid; }

    public BigDecimal getIncrement() { return increment; }
    public void setIncrement(BigDecimal increment) { this.increment = increment; }

    public LocalDateTime getCreated_at() { return created_at; }
    public void setCreated_at(LocalDateTime created_at) { this.created_at = created_at; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "AutoBidProfile{id=" + id + ", user_id=" + user_id
                + ", auction_id=" + auction_id + ", max_bid=" + max_bid + ", active=" + active + "}";
    }
}
