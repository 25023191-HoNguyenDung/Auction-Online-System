package com.auction.common.protocol;

import java.math.BigDecimal;
// dữ liệu khi user gửi đi
public class PlaceBidReqPayload {
    private long auctionId; // sản phẩm
    private long bidderId; // người đặt giá
    private BigDecimal amount; // giá


    public PlaceBidReqPayload(long auctionId, long bidderId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(long auctionId) {
        this.auctionId = auctionId;
    }

    public long getBidderId() {
        return bidderId;
    }

    public void setBidderId(long bidderId) {
        this.bidderId = bidderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
