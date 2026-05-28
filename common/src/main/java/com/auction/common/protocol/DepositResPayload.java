package com.auction.common.protocol;

import java.math.BigDecimal;

public class DepositResPayload {

    private boolean success;
    private BigDecimal newBalance;  // Số dư mới sau khi nạp
    private String message;


    public DepositResPayload(boolean success, BigDecimal newBalance, String message) {
        this.success    = success;
        this.newBalance = newBalance;
        this.message    = message;
    }

    public boolean isSuccess()         { return success; }
    public BigDecimal getNewBalance()  { return newBalance; }
    public String getMessage()         { return message; }
    public void setSuccess(boolean success)          { this.success = success; }
    public void setNewBalance(BigDecimal newBalance) { this.newBalance = newBalance; }
    public void setMessage(String message)           { this.message = message; }
}