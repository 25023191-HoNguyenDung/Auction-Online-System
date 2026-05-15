package com.auction.server.pattern;

import com.auction.server.dao.AuctionDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;

import java.util.List;
import java.util.Optional;


public class AuctionManager {

    private static volatile AuctionManager instance;

    private final AuctionDao auctionDao;

    private AuctionManager(AuctionDao auctionDao) {
        this.auctionDao = auctionDao;
    }


    public static synchronized void init(AuctionDao auctionDao) {
        if (instance != null)
            throw new IllegalStateException("AuctionManager has already been initialized");
        if (auctionDao == null)
            throw new IllegalArgumentException("AuctionDao cannot be null");
        instance = new AuctionManager(auctionDao);
    }

    public static AuctionManager getInstance() {
        if (instance == null)
            throw new IllegalStateException("AuctionManager has not been initialized. Call AuctionManager.init(...) first.");
        return instance;
    }


    public Auction addAuction(Auction auction) {
        if (auction.getId() != 0 && auctionDao.findById(auction.getId()).isPresent())
            throw new IllegalArgumentException("Auction ID already exists: " + auction.getId());

        return auctionDao.save(auction);
    }


    public boolean removeAuction(long auctionId) {
        Optional<Auction> opt = auctionDao.findById(auctionId);
        if (opt.isEmpty()) {
            System.out.println("AuctionManager: Cannot find auction id = " + auctionId);
            return false;
        }

        AuctionStatus status = opt.get().getStatus();
        if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING)
            throw new IllegalStateException(
                    "Can not delete auction with status " + status + ". Please cancel the auction first.");

        return auctionDao.deleteById(auctionId);
    }


    public Auction findAuctionById(long auctionId) {
        return auctionDao.findById(auctionId).orElse(null);
    }

    
    public List<Auction> getAllAuctions() {
        return auctionDao.findAll();
    }

    
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        return auctionDao.findByStatus(status);
    }

   
    public void listAllAuctions() {
        List<Auction> auctions = auctionDao.findAll();
        if (auctions.isEmpty()) {
            System.out.println("AuctionManager: No auctions found.");
            return;
        }
        
        for (Auction a : auctions) {
            System.out.printf("ID: %d | item: %d | Status: %s | Current Price: %s%n ",
                    a.getId(), a.getItem_id(), a.getStatus(), a.getCurrent_price()); 
        }
    }
}