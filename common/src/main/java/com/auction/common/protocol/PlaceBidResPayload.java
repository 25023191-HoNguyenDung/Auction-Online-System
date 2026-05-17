package com.auction.common.protocol;

import java.math.BigDecimal;
import java.util.UUID;
// kết quả khi đặt giá
public class PlaceBidResPayload {
    private boolean accepted; // đặt giá thành công không
    private long auctionId; // sp nào
    private BigDecimal currentHighestBid; // giá cao nhất
    private long leaderBidderId; // người dẫn đầu

    public PlaceBidResPayload() {
    }

    public PlaceBidResPayload(
            boolean accepted,
            long auctionId,
            BigDecimal currentHighestBid,
            long leaderBidderId
    ) {
        this.accepted = accepted;
        this.auctionId = auctionId;
        this.currentHighestBid = currentHighestBid;
        this.leaderBidderId = leaderBidderId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(long auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public long getLeaderBidderId() {
        return leaderBidderId;
    }

    public void setLeaderBidderId(long leaderBidderId) {
        this.leaderBidderId = leaderBidderId;
    }
}
