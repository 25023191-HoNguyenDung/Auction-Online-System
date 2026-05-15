package com.auction.server.service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.dao.AuctionDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
//class quan ly logic phien dau gia
public class AuctionLogicManager {

    private final Auction auction;
    private final AuctionDao auctionDao; // Luu tru va truy xuat thong tin phien dau gia
    //Dung ReadWriteLock de tranh xung dot khi co nhieu nguoi cung tham gia dau gia
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public AuctionLogicManager(Auction auction, AuctionDao auctionDao) {
        if (auction == null || auctionDao == null) {
            throw new IllegalArgumentException("Auction and AuctionDao cannot be null. Check database connection.");   // Kiem tra null de tranh loi khi khoi tao doi tuong AuctionLogicManager    
        }
        this.auction = auction;
        this.auctionDao = auctionDao;
    }
    //Dat gia
    public void placeBid(BidTransaction bid) throws AuctionMisMatchException,AuctionTimeException, InvalidBidException, AuctionConnectException {
        rwLock.writeLock().lock();
        try {
            if (bid.getAuctionId() != auction.getId()) {
                throw new AuctionMisMatchException("Auction ID does not match.");
            }
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionTimeException("Auction has not started or has ended.");
            }
            BigDecimal bidAmount = bid.getBidAmount();
            BigDecimal currentPrice = auction.getCurrent_price();
            long bidderId = bid.getBidderId();
            boolean isFirstBidder = (auction.getWinner_bidder_id() == 0);
            if (isFirstBidder) {
                if (bidAmount.compareTo(currentPrice) < 0) {
                    throw new InvalidBidException("Bid amount must be higher than or equal to the current price.");
                }
            } else {
                if (bidAmount.compareTo(currentPrice) <= 0) {
                    throw new InvalidBidException("Bid amount must be higher than the current price.");
                }
            }
            auction.setCurrent_price(bidAmount);
            auction.setWinner_bidder_id(bidderId);
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }
    //cap nhat trang thai
    public void updateAuctionStatus() throws AuctionConnectException {
        rwLock.writeLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            boolean isChanged = false;
            //Kiem tra trang thai: OPEN -> RUNNING
            if (auction.getStatus() == AuctionStatus.OPEN && now.isAfter(auction.getStart_time())) {
                auction.setStatus(AuctionStatus.RUNNING);
                System.out.println("Auction started.");
                isChanged = true;
            } 
            //RUNNING -> FINISHED
            else if (auction.getStatus() == AuctionStatus.RUNNING && now.isAfter(auction.getEnd_time())) {
                auction.setStatus(AuctionStatus.FINISHED);
                System.out.println("Auction finished.");
                System.out.println("Winner bidder ID: " + auction.getWinner_bidder_id());
                isChanged = true;
            }
            if (isChanged) {
                saveAuction("updateAuctionStatus"); // Luu trang thai moi cua phien dau gia sau khi cap nhat
            }
        } 
        finally {
            rwLock.writeLock().unlock();
        }
    }
    //Payment sau khi ket thuc dau gia
    public void payment() throws AuctionTimeException, AuctionConnectException {
        rwLock.writeLock().lock();
        try {
            if (auction.getStatus() != AuctionStatus.FINISHED) {
                throw new AuctionTimeException("Auction has not finished.");
            }
            long winnerId = auction.getWinner_bidder_id();
            if (winnerId == 0) {
                System.out.println("No winner found. Auction finished.");
                return;
            }
            else {
                long winner = auction.getWinner_bidder_id();
                System.out.println("Processing payment for the winning bidder: " + winner);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public void cancelled() throws AuctionTimeException, AuctionConnectException {
        rwLock.writeLock().lock();
        try {  
            if (auction.getStatus() != AuctionStatus.OPEN && auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionTimeException("Only auctions in OPEN or RUNNING status can be cancelled.");
            }
            auction.setStatus(AuctionStatus.CANCELLED);
            saveAuction("Cancelled");
            System.out.println("Auction " + auction.getId() + " has been cancelled.");
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    private void saveAuction(String action) throws AuctionConnectException {
        try {
            auctionDao.update(auction);
            System.out.println("Auction saved successfully after " + action + ".");
        } catch (Exception e) {
            throw new AuctionConnectException("Failed to save auction: " + e.getMessage());
        }
    }
}
