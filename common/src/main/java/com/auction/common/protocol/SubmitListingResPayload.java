package com.auction.common.protocol;

public class SubmitListingResPayload {
    private boolean success;
    private String message;
    private long auctionId;

    public SubmitListingResPayload() {}

    public SubmitListingResPayload(boolean success, String message, long auctionId) {
        this.success = success;
        this.message = message;
        this.auctionId = auctionId;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }
}
