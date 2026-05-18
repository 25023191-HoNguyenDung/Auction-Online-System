package com.auction.server.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.auction.common.exception.AuctionConnectException;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
// thao tác làm việc với db cho Auction
public interface AuctionDao {
    Optional<Auction> findById(long id);
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findAll();
    List<Auction> findExpiredRunning(); // tìm các phiên đag running
    Auction save(Auction auction) throws AuctionConnectException, SQLException;
    Auction update(Auction auction) throws AuctionConnectException, SQLException;
    boolean deleteById(long id);
}
