package com.auction.common.protocol;

import java.math.BigDecimal;

/**
 * FILE MỚI - Payload cho yêu cầu nạp tiền từ client lên server.
 *
 * PATH: common/src/main/java/com/auction/common/protocol/DepositReqPayload.java
 */
public class DepositReqPayload {

    private long userId;
    private BigDecimal amount;

    // Constructor mặc định cho JSON deserialize
    public DepositReqPayload() {}

    public DepositReqPayload(long userId, BigDecimal amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public long getUserId()        { return userId; }
    public BigDecimal getAmount()  { return amount; }
    public void setUserId(long userId)           { this.userId = userId; }
    public void setAmount(BigDecimal amount)     { this.amount = amount; }
}