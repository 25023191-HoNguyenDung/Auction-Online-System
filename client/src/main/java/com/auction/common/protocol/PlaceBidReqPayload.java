package com.auction.common.protocol;

import java.math.BigDecimal;
import java.util.UUID;
// dữ liệu khi user gửi đi
public class PlaceBidReqPayload {
    private UUID auctionId; // sản phẩm
    private UUID bidderId; // người đặt giá
    private BigDecimal amount; // giá

    public PlaceBidReqPayload() {
    }

    public PlaceBidReqPayload(UUID auctionId, UUID bidderId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public void setBidderId(UUID bidderId) {
        this.bidderId = bidderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
