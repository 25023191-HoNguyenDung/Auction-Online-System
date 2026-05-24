package com.auction.common.protocol;

import java.util.List;
// server trả về danh sách hiển thị và tổng số phiên đấu giá
public class ListAuctionsResPayload {
    private List<AuctionSummaryItem> auctions; // ds các phiên đấu giá
    private int total; // tổng số phiên đấu giá

    // Constructor rỗng cho Jackson giải mã JSON
    public ListAuctionsResPayload() {
    }

    public ListAuctionsResPayload(List<AuctionSummaryItem> auctions, int total) {
        this.auctions = auctions;
        this.total = total;
    }

    public List<AuctionSummaryItem> getAuctions() {
        return auctions;
    }

    public void setAuctions(List<AuctionSummaryItem> auctions) {
        this.auctions = auctions;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
