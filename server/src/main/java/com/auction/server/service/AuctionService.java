package com.auction.server.service;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;

import java.math.BigDecimal;
import java.util.List;
// định nghĩa nghiệp vụ chính của đấu giá
public interface AuctionService {
    List<Auction> getAllAuctions() throws AuctionConnectException;
    List<Auction> getAuctionsByStatus(AuctionStatus status) throws AuctionConnectException;
    Auction getAuctionById(long auctionId) throws AuctionConnectException;
    Auction updateAuction(Auction auction) throws AuctionConnectException;
    // đặt giá cho 1 phiên đgia
    BidTransaction placeBid(long auctionId, long bidderId, BigDecimal amount) throws AuctionMisMatchException, AuctionTimeException, InvalidBidException, AuctionConnectException;
    // lấy lsu bid 1 phiên theo tg
    List<BidTransaction> getBidHistory(long auctionId);
    BidTransaction getHighestBid(long auctionId);
    void closeAuction(long auctionId);
}
