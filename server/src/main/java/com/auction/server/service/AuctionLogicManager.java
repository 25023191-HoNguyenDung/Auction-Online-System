package com.auction.server.service;

import java.sql.SQLException;
import java.time.LocalDateTime;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.dao.AuctionDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
import com.auction.server.pattern.BidStrategy;
//class quan ly logic phien dau gia
public class AuctionLogicManager {

    private final Auction auction;
    private final AuctionDao auctionDao; // Luu tru va truy xuat thong tin phien dau gia
    private final BidStrategy bidStrategy = new BidStrategy(); // Chua cac quy tac lien quan den dat gia va cap nhat phien dau gia
    //Khai báo lock chung của hệ thống
    private final AuctionLockManager lockManager = AuctionLockManager.getInstance();

    public AuctionLogicManager(Auction auction, AuctionDao auctionDao) {
        if (auction == null || auctionDao == null) {
            throw new IllegalArgumentException("Auction and AuctionDao cannot be null. Check database connection.");   // Kiem tra null de tranh loi khi khoi tao doi tuong AuctionLogicManager    
        }
        this.auction = auction;
        this.auctionDao = auctionDao;
    }

    //Dat gia
    public void placeBid(BidTransaction bid) throws AuctionMisMatchException,AuctionTimeException, InvalidBidException, AuctionConnectException, SQLException {
        bidStrategy.validate(auction, bid); // Kiem tra tinh hop le cua giao dich dat gia
        bidStrategy.updateAuctionAfterBid(auction, bid); // Cap nhat thong tin phien dau gia sau moi lan dat gia
        bidStrategy.applyAntiSniping(auction); // Kiem tra va kich hoat anti-sniping neu can thiet
        saveAuction("placeBid"); // Luu thong tin phien dau gia sau khi dat gia thanh cong
    }

    //cap nhat trang thai
    public void updateAuctionStatus() throws AuctionConnectException, SQLException {
        lockManager.lock(auction.getId());
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
            lockManager.unlock(auction.getId());
        }
    }

    //Payment sau khi ket thuc dau gia
    public void payment() throws AuctionTimeException, AuctionConnectException {
        lockManager.lock(auction.getId());
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
            lockManager.unlock(auction.getId());
        }
    }

    //Hủy phiên
    public void cancelled() throws AuctionTimeException, AuctionConnectException, SQLException {
        lockManager.lock(auction.getId());
        try {  
            if (auction.getStatus() != AuctionStatus.OPEN && auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionTimeException("Only auctions in OPEN or RUNNING auctions can be cancelled. Current status: " + auction.getStatus());
            }
            auction.setStatus(AuctionStatus.CANCELLED);
            saveAuction("Cancelled");
            System.out.println("[Cancel] Auction " + auction.getId() + " has been cancelled.");
        } finally {
            lockManager.unlock(auction.getId());
        }
    }

      public void close() throws AuctionTimeException, AuctionConnectException, SQLException {
        lockManager.lock(auction.getId());
        try {
            if (auction.getStatus() != AuctionStatus.OPEN && auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionTimeException("Only OPEN or RUNNING auctions can be closed. Current status: " + auction.getStatus());
            }
            auction.setStatus(AuctionStatus.FINISHED);
            saveAuction("Close");
            System.out.println("[Admin] Auction " + auction.getId() + " closed. Winner: " + auction.getWinner_bidder_id());
        } finally {
            lockManager.unlock(auction.getId());
        }
    }

    public void open() throws AuctionTimeException, AuctionConnectException, SQLException {
        lockManager.lock(auction.getId());
        try {
            if (auction.getStatus() != AuctionStatus.OPEN) {
                throw new AuctionTimeException("Only OPEN auctions can be started. Current status: " + auction.getStatus());
            }
            auction.setStatus(AuctionStatus.RUNNING);
            saveAuction("Open");
            System.out.println("[Admin] Auction " + auction.getId() + " is now RUNNING. Starting price: " + auction.getStarting_price());
        } finally {
            lockManager.unlock(auction.getId());
        }
    }

    public void saveAuction(String action) throws AuctionConnectException, SQLException {
        try {
            auctionDao.update(auction);
            System.out.println("[AuctionLogicManager] DB save successful after action: " + action);
        } catch (SQLException e) {
            throw new AuctionConnectException("Failed to save DB for auction id: " + auction.getId() + "after: " + e.getMessage());
        }
    }

    //Getters de ServiceImpl doc trang thai hien tai cua phien dau gia
    public AuctionStatus getStatus() {
        lockManager.lock(auction.getId());
        try {
            return auction.getStatus();
        } finally {
            lockManager.unlock(auction.getId());
        }
    }

    public Auction getAuction() {
        return this.auction;
    }
    public long getAuctionId() {
        return auction.getId();
    }

}
