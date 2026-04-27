package com.auction.common.protocol;

import java.util.List;
// server trả về danh sách hiển thị và tổng số phiên đấu giá
public class ListAuctionsResPayload {
    private final List<AuctionSummaryItem> auctions; // ds các phiên đấu giá
    private final int total; // tổng số phiên đấu giá

    public ListAuctionsResPayload(List<AuctionSummaryItem> auctions, int total) {
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
