package com.auction.server.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.concurrency.AuctionLockManager;
import com.auction.server.concurrency.TransactionManager;
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
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.observer.AuctionEvent;
import com.auction.server.observer.AuctionEventPublisher;

// trung tâm xử lý nghiệp vụ đgia
public class AuctionServiceImpl implements AuctionService {

    private final AuctionDao auctionDao; // Lưu trữ và truy xuất thông tin phiên đấu giá
    private final BidDao bidDao; // Lưu trữ và truy xuất thông tin giao dịch đặt giá
    private final UserDao userDao; // Lưu trữ và truy xuất thông tin người dùng
    // Cache xử lí các nghiệp vụ đấu giá
    private final ConcurrentHashMap<Long, AuctionLogicManager> managerCache = new ConcurrentHashMap<>(); // Cache để lưu trữ các phiên đấu giá đang hoạt động
    private final AutoBidService autoBidService;
    private final AuctionEventPublisher publisher;
    //Quản lý transaction thông qua cơ chế Lambda
    private final TransactionManager transManager;

    public AuctionServiceImpl(AuctionDao auctionDao, BidDao bidDao, UserDao userDao) {
        this.auctionDao = auctionDao;
        this.bidDao = bidDao;
        this.userDao = userDao;
        this.autoBidService = new AutoBidService(this);
        this.publisher = AuctionEventPublisher.getInstance();   // Khởi tạo mặc định an toàn
        this.transManager = TransactionManager.getInstance();   // Khởi tạo mặc định an toàn
    }

    //Constructor đầy đủ tham số dùng cho test
    public AuctionServiceImpl(AuctionDao auctionDao, BidDao bidDao, UserDao userDao, AutoBidService autoBidService, AuctionEventPublisher publisher, TransactionManager transManager) {
        this.auctionDao = auctionDao;
        this.bidDao = bidDao;
        this.userDao = userDao;
        this.autoBidService = autoBidService;
        this.publisher = publisher;
        this.transManager = transManager;
    }

    // Constructor mặc định sử dụng các DAO JDBC để kết nối với cơ sở dữ liệu
    public AuctionServiceImpl() {
        this(new JdbcAuctionDao(), new JdbcBidDao(), new JdbcUserDao());
    }

    // Lấy bộ quản lý logic của phiên đấu giá - luôn nạp mới từ cơ sở dữ liệu
    // để tránh các lỗi không đồng bộ dữ liệu (stale cache) sau khi Admin phê duyệt hoặc đặt giá.
    private AuctionLogicManager getManager(long auctionId) {
        Auction auction = auctionDao.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));
        return new AuctionLogicManager(auction, auctionDao);
    }

    // Dọn cache - Không cần thiết vì không còn sử dụng cache trong getManager()
    private void clearCached(long auctionId) {
        // No-op
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
        return auctionDao.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));
    }

    @Override
    public Auction createAuction(Auction auction) throws AuctionConnectException {
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

    //Hàm đặt giá nội bộ dùng bên trong hệ thống, chỉ lưu xuống DB - không gọi processAutoBids trong AutoBidService
    @Override
    public BidTransaction placeBidInternal(long auctionId, long bidderId, BigDecimal amount)
            throws AuctionMisMatchException, AuctionTimeException, InvalidBidException, AuctionConnectException {
        User user = userDao.findById(bidderId)
                .orElseThrow(() -> new RuntimeException("User not found: " + bidderId));
        if (!(user instanceof Bidder bidder)) {
            throw new RuntimeException("User is not a bidder: " + bidderId);
        }

        //Kiểm tra số dư trước khi đặt giá
        if (!bidder.hasEnoughBalance(amount)) {
            throw new InvalidBidException("Insufficient balance. Available: "
                    + bidder.getAccount_balance() + ", Required: " + amount);
        }

        // Gọi LockManager để lấy khóa
        AuctionLockManager lockManager = AuctionLockManager.getInstance();
        lockManager.lock(auctionId);
        try {
            Auction currentAuction = auctionDao.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found: " + auctionId));
            long previousWinnerId = currentAuction.getWinner_bidder_id();
            BigDecimal previousWinAmount = currentAuction.getCurrent_price();

             // Kiểm tra số dư sau lock (để tránh double-spend)
            if (!bidder.hasEnoughBalance(amount)) {
                throw new InvalidBidException("Insufficient balance. Available: "
                        + bidder.getAccount_balance() + ", Required: " + amount);
            }

            BidTransaction bid = new BidTransaction(auctionId, bidder, amount);
            AuctionLogicManager manager = getManager(auctionId);
            try {
            transManager.executeInTransaction(conn -> {
                //Hoàn tiền cho người bị outbid (nếu có)
                if (previousWinnerId != 0 && previousWinnerId != bidderId) {
                    User prevUser = userDao.findById(previousWinnerId).orElse(null);
                    if (prevUser instanceof Bidder prevBidder) {
                        prevBidder.refundBid(previousWinAmount);
                        userDao.update(prevBidder);
                        System.out.println("[Refund] Bidder " + previousWinnerId
                                + " refunded " + previousWinAmount + " (outbid by " + bidderId + ")");
                    }
                }

                manager.placeBid(bid);  //Thực thi luật đặt giá

                //Trừ tiền người đặt giá mới
                bidder.deductForBid(amount);
                userDao.update(bidder); //Cập nhật thông tin số dư người dùng

                bidDao.save(bid);       //Lưu giao dịch đặt giá vào DB
            });
            } catch (RuntimeException e) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof AuctionMisMatchException) throw (AuctionMisMatchException) cause;
                    if (cause instanceof AuctionTimeException) throw (AuctionTimeException) cause;
                    if (cause instanceof InvalidBidException) throw (InvalidBidException) cause;
                    if (cause instanceof AuctionConnectException) throw (AuctionConnectException) cause;
                    if (cause instanceof java.sql.SQLException) throw new AuctionConnectException("Database error: " + cause.getMessage());
                }
                throw e;
            } 
            return bid;
        } finally {
            lockManager.unlock(auctionId);
        }
    }
    // Đặt giá cho phiên đấu giá - dùng cho client
    @Override
    public BidTransaction placeBid(long auctionId, long bidderId, BigDecimal amount)
            throws AuctionMisMatchException, AuctionTimeException, InvalidBidException, AuctionConnectException {
        //Thực hiện đặt giá và lưu DB thông qua hàm nội bộ
        BidTransaction bid = placeBidInternal(auctionId, bidderId, amount);
        //Sau khi ddawtj giá thành công, đọc lại DB để lấy thông tin phiên đấu giá mới nhất
        Auction updated = auctionDao.findById(auctionId).orElseThrow();
        //Thông báo cho các client khác về việc đặt giá mới thông qua cơ chế Observer
        publisher.publish(AuctionEvent.bidPlaced(auctionId, updated.getCurrent_price(), bid.getBidderId()));
        // kích hoạt auto-bid
        autoBidService.processAutoBids(auctionId, amount, bidderId);
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

    // Update auction status
    @Override
    public void checkStatus(long auctionId) throws AuctionConnectException {
        AuctionLogicManager manager = getManager(auctionId);
        try {
            AuctionStatus oldStatus = manager.getStatus();
            manager.updateAuctionStatus();
            AuctionStatus newStatus = manager.getStatus();
            
            // If transitioned from RUNNING to FINISHED, trigger automatic payment
            if (oldStatus == AuctionStatus.RUNNING && newStatus == AuctionStatus.FINISHED) {
                processPayment(auctionId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Database error while checking status for auction "
                    + auctionId + ": " + e.getMessage(), e);
        }
        clearCached(auctionId); // Clear cache because auction is finished
    }

    // Process payment after auction finishes
    @Override
    public void processPayment(long auctionId) throws AuctionTimeException, AuctionConnectException {
        AuctionLogicManager manager = getManager(auctionId);
        try {
            manager.payment(); // logs
            
            Auction auction = manager.getAuction();
            if (auction.getStatus() != AuctionStatus.FINISHED) {
                return; // Only process if currently FINISHED
            }
            
            long winnerId = auction.getWinner_bidder_id();
            long sellerId = auction.getSeller_id();
            BigDecimal amount = auction.getCurrent_price();
            
            if (winnerId > 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
                transManager.executeInTransaction(conn -> {
                    try {
                        // 1. Winner's balance is already deducted when placing the bid, no need to deduct it again.

                        
                        // 2. Credit seller's balance
                        User sellerOpt = userDao.findById(sellerId).orElse(null);
                        if (sellerOpt instanceof Seller seller) {
                            seller.receivePayment(amount);
                            userDao.update(seller);
                        }
                        
                        // 3. Mark auction as PAID
                        auction.setStatus(AuctionStatus.PAID);
                        auctionDao.update(auction);
                        
                        System.out.println("[Payment] Processed payment of " + amount 
                            + " from Bidder " + winnerId + " to Seller " + sellerId);
                    } catch (Exception e) {
                        throw new RuntimeException("Database transaction failed: " + e.getMessage(), e);
                    }
                });
            } else {
                // No winner, transition status directly to PAID
                transManager.executeInTransaction(conn -> {
                    try {
                        auction.setStatus(AuctionStatus.PAID);
                        auctionDao.update(auction);
                        System.out.println("[Payment] Auction " + auctionId + " closed with no winner - marked as PAID.");
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to update status: " + e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            throw new AuctionConnectException("Database error during payment for auction "
                    + auctionId + ": " + e.getMessage());
        }
    }

    // Cancel auction
    @Override
    public void cancelAuction(long auctionId) throws AuctionTimeException, AuctionConnectException {
        AuctionLogicManager manager = getManager(auctionId);
        try {
            manager.cancelled();
        } catch (AuctionTimeException e) {
            throw new RuntimeException("Cannot cancel auction. Auction time error: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("Database error during cancellation of auction "
                    + auctionId + ": " + e.getMessage(), e);
        }
        clearCached(auctionId); // Clear cache because auction is CANCELLED
    }

    @Override
    public void openAuction(long auctionId) throws AuctionConnectException {
        AuctionLogicManager manager = getManager(auctionId);
        try {
            manager.open(); 
        } catch (SQLException e) {
            throw new RuntimeException("Database error during opening of auction "
                    + auctionId + ": " + e.getMessage(), e);
        } catch (AuctionTimeException e) {         
            throw new RuntimeException("Cannot open auction: " + e.getMessage(), e);
        }
        Auction auction = getAuctionById(auctionId);
        publisher.publish(AuctionEvent.auctionStarted(auctionId, auction.getStarting_price()));
    }

    @Override
    // Close auction normally -> FINISHED, holds winner
    public void closeAuction(long auctionId) throws AuctionConnectException {
        AuctionLogicManager manager = getManager(auctionId);
        try {
            manager.close();
            // Process payment settlement immediately
            processPayment(auctionId);
        } catch (SQLException e) {
            throw new RuntimeException("Database error during closing of auction "
                    + auctionId + ": " + e.getMessage(), e);
        } catch (AuctionTimeException e) {
            throw new RuntimeException("Cannot close auction: " + e.getMessage(), e);
        }
        clearCached(auctionId);
    }
    
}