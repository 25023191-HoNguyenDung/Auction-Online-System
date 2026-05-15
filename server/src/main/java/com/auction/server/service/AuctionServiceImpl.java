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
import com.auction.server.dao.UserDao;
import com.auction.server.dao.jdbc.JdbcAuctionDao;
import com.auction.server.dao.jdbc.JdbcBidDao;
import com.auction.server.dao.jdbc.JdbcUserDao;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.BidTransaction;
import com.auction.server.model.Bidder;
import com.auction.server.model.User;

public class AuctionServiceImpl implements AuctionService {

    private final AuctionDao auctionDao; // Lưu trữ và truy xuất thông tin phiên đấu giá
    private final BidDao bidDao; // Lưu trữ và truy xuất thông tin giao dịch đặt giá    
    private final UserDao userDao; // Lưu trữ và truy xuất thông tin người dùng
    private final ConcurrentHashMap<Long, AuctionLogicManager> managerCache = new ConcurrentHashMap<>(); // Cache để lưu trữ các phiên đấu giá đang hoạt động
    
    public AuctionServiceImpl(AuctionDao auctionDao, BidDao bidDao, UserDao userDao) {
        this.auctionDao = auctionDao;
        this.bidDao = bidDao;
        this.userDao = userDao;
    }
    // Constructor mặc định sử dụng các DAO JDBC để kết nối với cơ sở dữ liệu
    public AuctionServiceImpl() {
        this(new JdbcAuctionDao(), new JdbcBidDao(), new JdbcUserDao());
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
}