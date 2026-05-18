package com.auction.server.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidDao;
import com.auction.server.dao.TransactionDao;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.jdbc.JdbcAuctionDao;
import com.auction.server.dao.jdbc.JdbcBidDao;
import com.auction.server.dao.jdbc.JdbcTransactionDao;
import com.auction.server.dao.jdbc.JdbcUserDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
import com.auction.server.model.Bidder;
import com.auction.server.model.Transaction;
import com.auction.server.model.User;

public class AuctionServiceImpl implements AuctionService {

    private final AuctionDao auctionDao;
    private final BidDao bidDao;
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final ConcurrentHashMap<Long, AuctionLogicManager> managerCache = new ConcurrentHashMap<>();

    public AuctionServiceImpl(AuctionDao auctionDao, BidDao bidDao, UserDao userDao, TransactionDao transactionDao) {
        this.auctionDao = auctionDao;
        this.bidDao = bidDao;
        this.userDao = userDao;
        this.transactionDao = transactionDao;
    }

    public AuctionServiceImpl() {
        this(new JdbcAuctionDao(), new JdbcBidDao(), new JdbcUserDao(), new JdbcTransactionDao());
    }
    // Lấy bộ quản lý logic của phiên đấu giá từ cache hoặc tạo mới nếu chưa tồn tại
    private AuctionLogicManager getManager(long auctionId) {
        AuctionLogicManager manager = managerCache.get(auctionId);
        if (manager == null) {
            Auction auction = auctionDao.findById(auctionId).orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));
            manager = new AuctionLogicManager(auction, auctionDao);
            managerCache.put(auctionId, manager);
        }
        return manager;
    }
    // Dọn cache khi phiên đấu giá kết thúc hoặc bị hủy
    private void clearCached(long auctionId) {
        AuctionLogicManager manager = managerCache.get(auctionId);
        //Nếu phiên đấu không xuất hiện trong cache 
        if (manager == null) return;
        AuctionStatus status = manager.getStatus();
        //Nếu phiên đã kết thúc hoặc bị hủy thì xóa khỏi cache để giải phóng bộ nhớ 
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELLED) {
            managerCache.remove(auctionId);
        }
    }
    // Quản lý phiên đấu giá
    @Override 
    public List<Auction> getAllAuctions() {
        return auctionDao.findAll();
    }
    @Override
    public List<Auction> getAuctionsByStatus(AuctionStatus status) {
        return auctionDao.findByStatus(status);
    }
    @Override
    public Auction getAuctionById(long auctionId) {
        return auctionDao.findById(auctionId).orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));
    }
    @Override 
    public Auction createAuction(Auction auction) throws AuctionConnectException{
        try {
            auction.setStatus(AuctionStatus.OPEN);
            auction.setCurrent_price(auction.getStarting_price());
            return auctionDao.save(auction);
        } catch (SQLException e) {
            throw new AuctionConnectException("Failed to create auction: " + e.getMessage());
        }
    }
    @Override
    public Auction updateAuction(Auction auction) throws AuctionConnectException {
        try {
            return auctionDao.update(auction);
        } catch (SQLException e) {
            throw new AuctionConnectException("Failed to update auction: " + e.getMessage());
        }
    }
    // Đặt giá cho phiên đấu giá
    @Override
    public BidTransaction placeBid(long auctionId, long bidderId, BigDecimal amount) throws AuctionMisMatchException, AuctionTimeException, InvalidBidException, AuctionConnectException {
        User user = userDao.findById(bidderId).orElseThrow(() -> new RuntimeException("User not found: " + bidderId));
        if (!(user instanceof Bidder bidder)) {
            throw new RuntimeException("User is not a bidder: " + bidderId);
        }
        BidTransaction bid = new BidTransaction(auctionId, bidder, amount);
        AuctionLogicManager manager = getManager(auctionId);
        try {
            manager.placeBid(bid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to place bid: " + e.getMessage(), e);
        }
        BidTransaction previousHighest = getHighestBid(auctionId);
        if (previousHighest != null) {
            refundBidBalance(
                previousHighest.getBidderId(),
                auctionId,
                previousHighest.getBidAmount().doubleValue()
            );
        }
        holdBalanceForBid(bidderId, auctionId, amount.doubleValue());
        bidDao.save(bid);
        return bid;
    }
    @Override
    public List<BidTransaction> getBidHistory(long auctionId) {
        return bidDao.findByAuctionId(auctionId);
    }
    @Override
    public BidTransaction getHighestBid(long auctionId) {   
        return bidDao.findHighestBidByAuctionId(auctionId).orElse(null);
    }
    //Cập nhật trạng thái phiên đấu giá
    @Override
    public void checkStatus(long auctionId) throws AuctionConnectException {
        AuctionLogicManager manager = getManager(auctionId);
        try {
            manager.updateAuctionStatus();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while checking status for auction " + auctionId + ": " + e.getMessage(), e);
        }
        clearCached(auctionId); // Dọn cache vì phiên đã FINISHED
        
    }
    // Xử lí thanh toán sau khi kết thúc phiên đấu giá
    @Override
    public void processPayment(long auctionId) throws AuctionTimeException, AuctionConnectException {
        AuctionLogicManager manager = getManager(auctionId);
        try {
            manager.payment();
        } catch (AuctionTimeException e) {
            throw new RuntimeException("Cannot process payment. Auction time error: " + e.getMessage(), e);
        } catch (AuctionConnectException e) {
            throw new RuntimeException("Database error during payment for auction " + auctionId + ": " + e.getMessage(), e);
        }
    }
    // Hủy phiên đấu giá
    @Override
    public void cancelAuction(long auctionId) throws AuctionTimeException, AuctionConnectException {
        AuctionLogicManager manager = getManager(auctionId);
        try {
            manager.cancelled();
        } catch (AuctionTimeException e) {
            throw new RuntimeException("Cannot cancel auction. Auction time error: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("Database error during cancellation of auction " + auctionId + ": " + e.getMessage(), e);
        }
        clearCached(auctionId); // Dọn cache vì phiên đã CANCELLED
    }

    public void deposit(long userId, double amount) {
        JdbcUserDao userDao = (JdbcUserDao) this.userDao;
        double currentBalance = userDao.getBalance(userId);
        double newBalance = currentBalance + amount;
        userDao.updateBalance(userId, newBalance);
        Transaction transaction = new Transaction(userId, "DEPOSIT", amount, null, "User deposit");
        transactionDao.save(transaction);
    }

    public void withdraw(long userId, double amount) {
        JdbcUserDao userDao = (JdbcUserDao) this.userDao;
        double currentBalance = userDao.getBalance(userId);
        if (currentBalance < amount) {
            throw new RuntimeException("Insufficient balance. Current: " + currentBalance + ", Required: " + amount);
        }
        double newBalance = currentBalance - amount;
        userDao.updateBalance(userId, newBalance);
        Transaction transaction = new Transaction(userId, "WITHDRAWAL", amount, null, "User withdrawal");
        transactionDao.save(transaction);
    }

    public double getBalance(long userId) {
        JdbcUserDao userDao = (JdbcUserDao) this.userDao;
        return userDao.getBalance(userId);
    }

    public List<Transaction> getTransactionHistory(long userId) {
        return transactionDao.findByUserId(userId);
    }

    public void holdBalanceForBid(long userId, long auctionId, double bidAmount) {
        JdbcUserDao userDao = (JdbcUserDao) this.userDao;
        double currentBalance = userDao.getBalance(userId);
        if (currentBalance < bidAmount) {
            throw new RuntimeException("Insufficient balance. Current: " + currentBalance + ", Bid: " + bidAmount);
        }
        double newBalance = currentBalance - bidAmount;
        userDao.updateBalance(userId, newBalance);
        Transaction transaction = new Transaction(userId, "BID_HOLD", bidAmount, auctionId, "Balance held for bid");
        transactionDao.save(transaction);
    }

    public void refundBidBalance(long userId, long auctionId, double bidAmount) {
        JdbcUserDao userDao = (JdbcUserDao) this.userDao;
        double currentBalance = userDao.getBalance(userId);
        double newBalance = currentBalance + bidAmount;
        userDao.updateBalance(userId, newBalance);
        Transaction transaction = new Transaction(userId, "BID_REFUND", bidAmount, auctionId, "Bid refunded - outbid");
        transactionDao.save(transaction);
    }
}
