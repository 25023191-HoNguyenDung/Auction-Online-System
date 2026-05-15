package com.auction.server.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.auction.common.exception.AuctionConnectException;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;

public interface AuctionDao {
    Optional<Auction> findById(long id);
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findAll();
    /** Tìm các phiên RUNNING đã hết giờ — dùng cho AuctionClosingService */
    List<Auction> findExpiredRunning();
    Auction save(Auction auction) throws AuctionConnectException, SQLException;
    Auction update(Auction auction) throws AuctionConnectException, SQLException;
    boolean deleteById(long id);
}
