package com.auction.common.protocol;

import java.math.BigDecimal;
import java.util.UUID;
// kết quả khi đặt giá
public class PlaceBidResPayload {
    private boolean accepted; // đặt giá thành công không
    private UUID auctionId; // sp nào
    private BigDecimal currentHighestBid; // giá cao nhất
    private UUID leaderBidderId; // người dẫn đầu

    public PlaceBidResPayload() {
    }

    public PlaceBidResPayload(
            boolean accepted,
            UUID auctionId,
            BigDecimal currentHighestBid,
            UUID leaderBidderId
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

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public UUID getLeaderBidderId() {
        return leaderBidderId;
    }

    public void setLeaderBidderId(UUID leaderBidderId) {
        this.leaderBidderId = leaderBidderId;
    }
}
