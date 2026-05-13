package com.auction.server.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;


public class Auction {

    private long id;
    private long item_id;
    private long seller_id;
    private BigDecimal starting_price;
    private BigDecimal current_price;
    private AuctionStatus status;
    private LocalDateTime start_time;
    private LocalDateTime end_time;
    private long winner_bidder_id;

    // Constructor mặc định
    public Auction() {}

    // Constructor đầy đủ
    public Auction(long id, long item_id, long seller_id,
                   BigDecimal starting_price, BigDecimal current_price,
                   AuctionStatus status, LocalDateTime start_time,
                   LocalDateTime end_time, long winner_bidder_id) {

        this.id = id;
        this.item_id = item_id;
        this.seller_id = seller_id;
        this.starting_price = starting_price;
        this.current_price = current_price;
        this.status = status;
        this.start_time = start_time;
        this.end_time = end_time;
        this.winner_bidder_id = winner_bidder_id;
    }


    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getItem_id() { return item_id; }
    public void setItem_id(long item_id) { this.item_id = item_id; }

    public long getSeller_id() { return seller_id; }
    public void setSeller_id(long seller_id) { this.seller_id = seller_id; }

    public BigDecimal getStarting_price() { return starting_price; }
    public void setStarting_price(BigDecimal starting_price) { this.starting_price = starting_price; }

    public BigDecimal getCurrent_price() { return current_price; }
    public void setCurrent_price(BigDecimal current_price) { this.current_price = current_price; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public LocalDateTime getStart_time() { return start_time; }
    public void setStart_time(LocalDateTime start_time) { this.start_time = start_time; }

    public LocalDateTime getEnd_time() { return end_time; }
    public void setEnd_time(LocalDateTime end_time) { this.end_time = end_time; }

    public long getWinner_bidder_id() { return winner_bidder_id; }
    public void setWinner_bidder_id(long winner_bidder_id) { this.winner_bidder_id = winner_bidder_id; }

    @Override
    public String toString() {
        return "Auction{id=" + id + ", status=" + status + ", current_price=" + current_price + "}";
    }
}