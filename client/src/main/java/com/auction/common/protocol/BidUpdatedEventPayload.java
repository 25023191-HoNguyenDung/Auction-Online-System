package com.auction.common.protocol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
// gói thông tin và gửi cho hệ thông khác khi có giá mới
public class BidUpdatedEventPayload {
    private UUID auctionId;
    private BigDecimal newHighestBid;
    private UUID leaderBidderId;
    private Instant bidTime;

    public BidUpdatedEventPayload() {
    }

    public BidUpdatedEventPayload(
            UUID auctionId, // ID phiên đấu giá (mã định danh , ko bị trùng)
            BigDecimal newHighestBid, // giá cao nhất mới(lưu tiền chính xác tuyệt đối)
            UUID leaderBidderId, // ID người trả cao nhất
            Instant bidTime // thời điểm đặt giá(lưu thời gian chính xác)
    ) {
        this.auctionId = auctionId;
        this.newHighestBid = newHighestBid;
        this.leaderBidderId = leaderBidderId;
        this.bidTime = bidTime;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getNewHighestBid() {
        return newHighestBid;
    }

    public void setNewHighestBid(BigDecimal newHighestBid) {
        this.newHighestBid = newHighestBid;
    }

    public UUID getLeaderBidderId() {
        return leaderBidderId;
    }

    public void setLeaderBidderId(UUID leaderBidderId) {
        this.leaderBidderId = leaderBidderId;
    }

    public Instant getBidTime() {
        return bidTime;
    }

    public void setBidTime(Instant bidTime) {
        this.bidTime = bidTime;
    }
}
