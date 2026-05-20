package com.auction.server.service;

import java.sql.SQLException;
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
import com.auction.server.pattern.BidStrategy;
//class quan ly logic phien dau gia
public class AuctionLogicManager {

    private final Auction auction;
    private final AuctionDao auctionDao; // Luu tru va truy xuat thong tin phien dau gia
    //Dung ReadWriteLock de tranh xung dot khi co nhieu nguoi cung tham gia dau gia
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final BidStrategy bidStrategy = new BidStrategy(); // Chua cac quy tac lien quan den dat gia va cap nhat phien dau gia

    public AuctionLogicManager(Auction auction, AuctionDao auctionDao) {
        if (auction == null || auctionDao == null) {
            throw new IllegalArgumentException("Auction and AuctionDao cannot be null. Check database connection.");   // Kiem tra null de tranh loi khi khoi tao doi tuong AuctionLogicManager    
        }
        this.auction = auction;
        this.auctionDao = auctionDao;
    }
    //Dat gia
    public void placeBid(BidTransaction bid) throws AuctionMisMatchException,AuctionTimeException, InvalidBidException, AuctionConnectException, SQLException {
        rwLock.writeLock().lock();
        try {
            bidStrategy.validate(auction, bid); // Kiem tra tinh hop le cua giao dich dat gia
            bidStrategy.updateAuctionAfterBid(auction, bid); // Cap nhat thong tin phien dau gia sau moi lan dat gia
            bidStrategy.applyAntiSniping(auction); // Kiem tra va kich hoat anti-sniping neu can thiet
            saveAuction("placeBid"); // Luu thong tin phien dau gia sau khi dat gia thanh cong
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }
    //cap nhat trang thai
    public void updateAuctionStatus() throws AuctionConnectException, SQLException {
        rwLock.writeLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            boolean isChanged = false;
            //Kiem tra trang thai: OPEN -> RUNNING
            if (auction.getStatus() == AuctionStatus.OPEN && !now.isBefore(auction.getStart_time())) {
                auction.setStatus(AuctionStatus.RUNNING);
                System.out.println("[Scheduler] Auction " + auction.getId() + " opened -> RUNNING.");
                isChanged = true;
            } 
            //RUNNING -> FINISHED
            else if (auction.getStatus() == AuctionStatus.RUNNING && now.isAfter(auction.getEnd_time())) {
                auction.setStatus(AuctionStatus.FINISHED);
                System.out.println("[Scheduler] Auction " + auction.getId() + " ended -> FINISHED.");
                System.out.println("Winner bidder ID: " + auction.getWinner_bidder_id());
                isChanged = true;
            }
            if (isChanged) {
                saveAuction("updateAuctionStatus"); // Luu trang thai moi cua phien dau gia sau khi cap nhat
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    //Payment sau khi ket thuc dau gia
    public void payment() throws AuctionTimeException, AuctionConnectException {
        rwLock.writeLock().lock();
        try {
            if (auction.getStatus() != AuctionStatus.FINISHED) {
                throw new AuctionTimeException("Only FINISHED auctions can process payment. Current status: " + auction.getStatus());
            }
            long winnerId = auction.getWinner_bidder_id();
            if (winnerId == 0) {
                System.out.println("[Payment] Auction " + auction.getId() + " has no winner — skipping payment.");
            }
            else {
                System.out.println("[Payment] Processing payment for winner id: " + winnerId);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public void cancelled() throws AuctionTimeException, AuctionConnectException, SQLException {
        rwLock.writeLock().lock();
        try {  
            if (auction.getStatus() != AuctionStatus.OPEN && auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionTimeException("Only auctions in OPEN or RUNNING auctions can be cancelled. Current status: " + auction.getStatus());
            }
            auction.setStatus(AuctionStatus.CANCELLED);
            saveAuction("Cancelled");
            System.out.println("[Cancel] Auction " + auction.getId() + " has been cancelled.");
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    private void saveAuction(String action) throws AuctionConnectException, SQLException {
        try {
            auctionDao.update(auction);
            System.out.println("[AuctionLogicManager] DB save successful after action: " + action);
        } catch (SQLException e) {
            throw new AuctionConnectException("Failed to save DB for auction id: " + auction.getId() + "after: " + e.getMessage());
        }
    }
    //Getters de ServiceImpl doc trang thai hien tai cua phien dau gia
    public AuctionStatus getStatus() {
        rwLock.readLock().lock();
        try {
            return auction.getStatus();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    public long getAuctionId() {
        return auction.getId();
    }

    public void close() throws AuctionTimeException, AuctionConnectException, SQLException {
        rwLock.writeLock().lock();
        try {
            if (auction.getStatus() != AuctionStatus.OPEN && auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionTimeException("Only OPEN or RUNNING auctions can be closed. Current status: " + auction.getStatus());
            }
            auction.setStatus(AuctionStatus.FINISHED);
            saveAuction("Close");
            System.out.println("[Admin] Auction " + auction.getId() + " closed. Winner: " + auction.getWinner_bidder_id());
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    public void open() throws AuctionTimeException, AuctionConnectException, SQLException {
        rwLock.writeLock().lock();
        try {
            if (auction.getStatus() != AuctionStatus.OPEN) {
                throw new AuctionTimeException("Only OPEN auctions can be started. Current status: " + auction.getStatus());
            }
            auction.setStatus(AuctionStatus.RUNNING);
            saveAuction("Open");
            System.out.println("[Admin] Auction " + auction.getId() + " is now RUNNING. Starting price: " + auction.getStarting_price());
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
