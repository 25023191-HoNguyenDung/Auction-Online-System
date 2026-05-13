package com.auction.server.service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
        this.auction = auction;
        this.auctionDao = auctionDao;
    }
    //Dat gia
    public void placeBid(BidTransaction bid) throws AuctionMisMatchException,AuctionTimeException, InvalidBidException {
        rwLock.writeLock().lock();
        try {
            if (!(bid.getAuctionId() ==(auction.getId()))) {
                throw new AuctionMisMatchException("ID phien dau gia khong khop.");
            }
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionTimeException("Phien dau gia chua bat dau hoac da ket thuc.");
            }
            BigDecimal bidAmount = bid.getBidAmount();
            BigDecimal currentPrice = auction.getCurrent_price();
            long bidderId = bid.getBidderId();
            boolean isFirstBidder = auction.getWinner_bidder_id() == 0; //Gia su 0 la chua co nguoi nao dat gia
                if (bidAmount.compareTo(currentPrice) <= 0) {
                    throw new InvalidBidException("Gia dat cua ban phai lon hon hoac bang gia hien tai.");
                }
            
            else {
                if (bidAmount.compareTo(currentPrice) <= 0) {
                    throw new InvalidBidException("Gia dat cua ban phai lon hon gia hien tai.");
                }
            }
            auction.setCurrent_price(bidAmount);
            auction.setWinner_bidder_id(bidderId);
            auctionDao.update(auction);
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
            boolean isChanged = false;
            //Kiem tra trang thai: OPEN -> RUNNING
            if (auction.getStatus() == AuctionStatus.OPEN && now.isAfter(auction.getEnd_time())) {
                auction.setStatus(AuctionStatus.RUNNING);
                System.out.println("Bat dau phien dau gia.");
                isChanged = true;
            } 
            //RUNNING -> FINISHED
            else if (auction.getStatus() == AuctionStatus.RUNNING && now.isAfter(auction.getEnd_time())) {
                auction.setStatus(AuctionStatus.FINISHED);
                System.out.println("Ket thuc phien dau gia.");
                System.out.println("ID nguoi thang cuoc: " + auction.getWinner_bidder_id());
                isChanged = true;
            }
            if (isChanged) {
                auctionDao.update(auction); // Luu trang thai moi cua phien dau gia vao database
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
            if (auction.getStatus() == AuctionStatus.FINISHED && auction.getWinner_bidder_id() != 0) {
                long winner = auction.getWinner_bidder_id();
                boolean isWinner = true; //Gia su co nguoi thang cuoc
                if (isWinner) {
                    System.out.println("Thanh toan cho nguoi thang cuoc: " + winner);
                } else {
                    System.out.println("Khong co nguoi thang cuoc nao.");
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
