package com.auction.common.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
// server trả về danh sách hiển thị và tổng số phiên đấu giá
public class ListAuctionsResPayload {
    private final List<AuctionSummaryItem> auctions; // ds các phiên đấu giá
    private final int total; // tổng số phiên đấu giá

    @JsonCreator
    public ListAuctionsResPayload(
            @JsonProperty("auctions") List<AuctionSummaryItem> auctions,
            @JsonProperty("total")    int total) {
        this.auctions = auctions;
        this.total = total;
    }

    public List<AuctionSummaryItem> getAuctions() {
        return auctions;
    }

    public int getTotal() {
        return total;
    }
}
