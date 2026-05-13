package com.auction.common.protocol;

import java.math.BigDecimal;
import java.time.Instant;
// thông báo khi phiên đấu giá kết thúc
public class AuctionClosedEventPayload {
    private final long auctionId; // phiên nào vừa kết thúc
    private final BigDecimal finalPrice;
    private final long winnerBidderId; // người thắng
    private final String status; // trạng thái
    private final Instant closedAt; // thời gian đóng

    public AuctionClosedEventPayload(long auctionId, BigDecimal finalPrice, long winnerBidderId, String status, Instant closedAt) {
        this.auctionId = auctionId;
        this.finalPrice = finalPrice;
        this.winnerBidderId = winnerBidderId;
        this.status = status;
        this.closedAt = closedAt;
    }

    public long getAuctionId() {
        return auctionId;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public long getWinnerBidderId() {
        return winnerBidderId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}

