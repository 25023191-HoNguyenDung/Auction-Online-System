package com.auction.server.observer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
// dữ liệu 1 sự kiện trong phiên đấu giá
public class AuctionEvent {

    public enum Type{
        BID_PLACED, // có bid mới hợp lệ
        AUCTION_CLOSED, // phiên kết thúc
        AUCTION_STARTED, // running
        PRICE_UPDATED // giá hiện tại thay đổi
    }

    private final Type type;
    private final long auctionId;
    private final BigDecimal currentPrice;
    private final long bidderID;
    private final String winnerBidderId; // có khi type = AUCTION_CLOSED
    private final LocalDateTime timestamp;

    public AuctionEvent(Type type, long auctionId, BigDecimal currentPrice, long bidderID, String winnerBidderId) {
        this.type = type;
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.bidderID = bidderID;
        this.winnerBidderId = winnerBidderId;
        this.timestamp = LocalDateTime.now();
    }
    // tạo khi có người đặt giá thành công
    public static AuctionEvent bidPlaced(long auctionId, BigDecimal newPrice, long bidderID){
        return new AuctionEvent(Type.BID_PLACED,auctionId,newPrice,bidderID,null);
    }
    // tạo khi phiên đấu giá kết thúc
    public static AuctionEvent auctionClosed(long auctionId, BigDecimal finalPrice, String winnerBidderId){
        return new AuctionEvent(Type.AUCTION_CLOSED, auctionId, finalPrice, 0, winnerBidderId);
    }
    // tạo khi phiên đấu giá bắt đầu
    public static AuctionEvent auctionStarted(long auctionId, BigDecimal startingPrice) {
        return new AuctionEvent(Type.AUCTION_STARTED, auctionId, startingPrice, 0, null);
    }
    // tạo khi giá được cập nhật
    public static AuctionEvent priceUpdated(long auctionId, BigDecimal newPrice, long bidderId) {
        return new AuctionEvent(Type.PRICE_UPDATED, auctionId, newPrice, bidderId, null);
    }

    public Type getType() {
        return type;
    }

    public long getAuctionId() {
        return auctionId;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public long getBidderID() {
        return bidderID;
    }

    public String getWinnerBidderId() {
        return winnerBidderId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "AuctionEvent{" +
                "type=" + type +
                ", auctionId=" + auctionId +
                ", currentPrice=" + currentPrice +
                ", bidderID=" + bidderID +
                ", winnerBidderId='" + winnerBidderId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
