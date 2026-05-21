package com.auction.server.service;

import java.math.BigDecimal;
import java.util.List;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
// định nghĩa nghiệp vụ chính của đấu giá
public interface AuctionService {
    
    List<Auction> getAllAuctions() throws AuctionConnectException;
    
    List<Auction> getAuctionsByStatus(AuctionStatus status) throws AuctionConnectException;
    
    Auction getAuctionById(long auctionId) throws AuctionConnectException;
    
    Auction updateAuction(Auction auction) throws AuctionConnectException;
    
    // Tạo phiên đấu giá mới
    Auction createAuction(Auction auction) throws AuctionConnectException; 
    
    // Đặt giá cho 1 phiên đấu giá
    BidTransaction placeBid(long auctionId, long bidderId, BigDecimal amount) throws AuctionMisMatchException, AuctionTimeException, InvalidBidException, AuctionConnectException;
    
    // Hàm xử lí đặt giá nội bộ (Internal)
    //Dùng chung cho đặt thủ công và tự động
    BidTransaction placeBidInternal(long auctionId, long BidderId, BigDecimal amout) throws AuctionMisMatchException, AuctionTimeException, InvalidBidException, AuctionConnectException;
    
    // lấy lịch sử bid 1 phiên theo thời gian
    List<BidTransaction> getBidHistory(long auctionId);
    
    BidTransaction getHighestBid(long auctionId);
    
    // Mở phiên đấu giá
    void openAuction(long auctionId) throws AuctionConnectException;
    
    // Cập nhật trạng thái phiên
    void checkStatus(long auctionId) throws AuctionConnectException;
    
    // Xử lí thanh toán sau khi kết thúc phiên đấu giá
    void processPayment(long auctionId) throws AuctionTimeException, AuctionConnectException;
    
    // Hủy phiên đấu giá 
    void cancelAuction(long auctionId) throws AuctionTimeException, AuctionConnectException;
    
    // Đóng phiên đấu giá
    void closeAuction(long auctionId) throws AuctionConnectException;
}
