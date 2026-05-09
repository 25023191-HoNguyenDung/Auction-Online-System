package com.auction.server.service;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
//class quan ly logic phien dau gia
public class AuctionLogicManager {

    private final Auction auction;
    //Dung ReadWriteLock de tranh xung dot khi co nhieu nguoi cung tham gia dau gia
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public AuctionLogicManager(Auction auction) {
        this.auction = auction;
    }
    //Dat gia
    public void placeBid(BidTransaction bid) throws AuctionMisMatchException,AuctionTimeException, InvalidBidException {
        rwLock.writeLock().lock();
        try {
            if (!bid.getAuctionId().equals(auction.getId())) {
                throw new AuctionMisMatchException("ID phien dau gia khong khop.");
            }
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionTimeException("Phien dau gia chua bat dau hoac da ket thuc.");
            }
            double bidAmount = bid.getBidAmount();
            double currentPrice = auction.getCurrentPrice();
            String bidderName = bid.getBidderName();
            boolean isFirstBidder = auction.getHighestBidder().equals("None");
            if (isFirstBidder) {
                if (bidAmount <= currentPrice) {
                    throw new InvalidBidException("Gia dat cua ban phai lon hon hoac bang gia hien tai.");
                }
            } 
            else {
                if (bidAmount <= currentPrice) {
                    throw new InvalidBidException("Gia dat cua ban phai lon hon gia hien tai.");
                }
            }
            auction.setCurrentPrice(bidAmount);
            auction.setHighestBidder(bidderName);
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }
    //cap nhat trang thai
    public void updateAuctionStatus() {
        rwLock.writeLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            //Kiem tra trang thai: OPEN -> RUNNING
            if (auction.getStatus() == AuctionStatus.OPEN && now.isAfter(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.RUNNING);
                System.out.println("Bat dau phien dau gia.");
            } 
            //RUNNING -> FINISHED
            else if (auction.getStatus() == AuctionStatus.RUNNING && now.isAfter(auction.getEndTime())) {
                auction.setStatus(AuctionStatus.FINISHED);
                System.out.println("Ket thuc phien dau gia.");
                System.out.println("Nguoi thang cuoc: " + auction.getHighestBidder());
            }
        } 
        finally {
            rwLock.writeLock().unlock();
        }
    }
    //Payment sau khi ket thuc dau gia
    public void payment() {
        rwLock.writeLock().lock();
        try {
            if (auction.getStatus() == AuctionStatus.FINISHED && !auction.getHighestBidder().equals("None")) {
                String winner = auction.getHighestBidder();
                boolean isWinner = true; //Gia su co nguoi
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
