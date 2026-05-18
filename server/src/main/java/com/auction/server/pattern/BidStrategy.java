//class để biểu diễn rule của autobid, có thể mở rộng để thêm các rule khác
package com.auction.server.pattern;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;

public class BidStrategy {
    private static final long ANTI_SNIPE_SECONDS = 30; // 30 giây cuối sẽ kích hoạt anti-snipe
    private static final long EXTENSION_TIME_SECONDS = 60; // mỗi lần kích hoạt sẽ gia hạn thêm 60 giây 

    //hàm kiểm tra tính hợp lệ của giao dịch đặt giá
    public void validate(Auction auction, BidTransaction bid) throws AuctionMisMatchException, AuctionTimeException, InvalidBidException {
        //kiểm tra phiên đấu giá có tồn tại hay không
        if (bid.getAuctionId() != auction.getId()) {
            throw new AuctionMisMatchException("Bid auction ID does not match with the auction");
        }
        //Kiểm tra trạng thái của phiên đấu giá
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionTimeException("Auction is not running. Auction status: " + auction.getStatus());
        }
        //Kiem tra số tiền đặt giá có hợp lệ hay không (bị âm, bằng 0 hoặc vi phạm luật đặt giá)
        validateBidAmount(auction, bid);
    }
    // Hàm kiểu tra tính hợp lệ của số tiền đặt giá dựa trên luật của phiên đấu giá
    private void validateBidAmount(Auction auction, BidTransaction bid) throws InvalidBidException {
        BigDecimal bidAmount = bid.getBidAmount();
        BigDecimal currentPrice = auction.getCurrent_price();
        //GIá phải là số dương
        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBidException("Bid amount must be greater than zero.");
        }
        //Xét người đầu tiên
        boolean isFirstBidder = (auction.getWinner_bidder_id() == 0);   //Nếu chưa có người fddawtj giá -> id = 0
        if (isFirstBidder) {//Nếu là người đầu tiên đặt giá
            if (bidAmount.compareTo(currentPrice) < 0) {
                throw new InvalidBidException("First bid must be at least the starting price.");
            }
        } else { //nếu không phải người đầu tiên đặt giá
            if (bidAmount.compareTo(currentPrice) <= 0) {
                throw new InvalidBidException("Bid amount must be greater than the current price.");
            }
        }
    }
    //Hàm cập nhật thông tin phiên đấu sau 1 lần đặt giá
    public void updateAuctionAfterBid(Auction auction, BidTransaction bid) {
        auction.setCurrent_price(bid.getBidAmount());
        auction.setWinner_bidder_id(bid.getBidderId());
    }
    //Hàm kích hoạt anti-sniping: nếu còn dưới 30s mới đặt giá thì gia hạn phiên đấu thêm 1p
    public boolean applyAntiSniping(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getEnd_time();
        //Tính khoảng thời gian từ now đến endTime -> trả về số giây hiện tại
        long secondsLeft = Duration.between(now, endTime).getSeconds(); 
        //Xét secondsLeft xem có nằm trong khoảng kích hoạt anti-sniping không
        if (secondsLeft >0 && secondsLeft <= ANTI_SNIPE_SECONDS) {
            //Gia hạn thêm 60s
            auction.setEnd_time(endTime.plusSeconds(EXTENSION_TIME_SECONDS));
            System.out.println("[AntiSnipe] Auction " + auction.getId() + " extended by " + EXTENSION_TIME_SECONDS + " seconds due to anti-sniping rule.");
            System.out.println("[AntiSnipe] New end time: " + auction.getEnd_time());
            return true;
        }
        return false;
    }
    public BigDecimal calculateAutoBid(Auction auction, BigDecimal bidStep, BigDecimal maxBid) {
        BigDecimal nextBid = auction.getCurrent_price().add(bidStep);
        if (nextBid.compareTo(maxBid) > 0) {
            if (auction.getCurrent_price().compareTo(maxBid) < 0) {
                return maxBid; // Nếu vượt quá giá giới hạn, đặt giá tối đa
            }
            return null; ////Nếu currentPrice >= maxBid -> không được đặt giá tiếp theo -> dừng AutoBid
        }
        return nextBid; 
    }
}
