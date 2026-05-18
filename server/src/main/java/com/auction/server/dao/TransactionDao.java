package com.auction.server.dao;

import java.util.List;
import java.util.Optional;

import com.auction.server.model.Transaction;

public interface TransactionDao {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(long id);
    List<Transaction> findByUserId(long userId);
    List<Transaction> findByAuctionId(long auctionId);
    List<Transaction> findAll();
}
