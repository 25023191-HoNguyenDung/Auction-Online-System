package com.auction.server.dao;

import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import java.util.List;
import java.util.Optional;

public interface AuctionDao {
    Optional<Auction> findById(long id);
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findAll();
    /** Tìm các phiên RUNNING đã hết giờ — dùng cho AuctionClosingService */
    List<Auction> findExpiredRunning();
    Auction save(Auction auction);
    Auction update(Auction auction);
    boolean deleteById(long id);
}
