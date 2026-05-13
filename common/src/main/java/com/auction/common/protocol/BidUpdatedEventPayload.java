package com.auction.common.protocol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
// gói thông tin và gửi cho hệ thông khác khi có giá mới
public class BidUpdatedEventPayload {
    private long auctionId;
    private BigDecimal newHighestBid;
    private long leaderBidderId;
    private Instant bidTime;

    public BidUpdatedEventPayload() {
    }

    public BidUpdatedEventPayload(
            long auctionId, // ID phiên đấu giá
            BigDecimal newHighestBid, // giá cao nhất mới(lưu tiền chính xác tuyệt đối)
            long leaderBidderId, // ID người trả cao nhất
            Instant bidTime // thời điểm đặt giá(lưu thời gian chính xác)
    ) {
        this.auctionId = auctionId;
        this.newHighestBid = newHighestBid;
        this.leaderBidderId = leaderBidderId;
        this.bidTime = bidTime;
    }

    public long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(long auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getNewHighestBid() {
        return newHighestBid;
    }

    public void setNewHighestBid(BigDecimal newHighestBid) {
        this.newHighestBid = newHighestBid;
    }

    public long getLeaderBidderId() {
        return leaderBidderId;
    }

    public void setLeaderBidderId(long leaderBidderId) {
        this.leaderBidderId = leaderBidderId;
    }

    public Instant getBidTime() {
        return bidTime;
    }

    public void setBidTime(Instant bidTime) {
        this.bidTime = bidTime;
    }
}
