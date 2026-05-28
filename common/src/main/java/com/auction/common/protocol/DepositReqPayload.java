package com.auction.common.protocol;

import java.math.BigDecimal;


public class DepositReqPayload {

    private long userId;
    private BigDecimal amount;


    public DepositReqPayload(long userId, BigDecimal amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public long getUserId()        { return userId; }
    public BigDecimal getAmount()  { return amount; }
    public void setUserId(long userId)           { this.userId = userId; }
    public void setAmount(BigDecimal amount)     { this.amount = amount; }
}