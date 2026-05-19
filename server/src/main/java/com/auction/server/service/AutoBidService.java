package com.auction.server.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.auction.common.exception.AuctionConnectException;
import com.auction.common.exception.AuctionMisMatchException;
import com.auction.common.exception.AuctionTimeException;
import com.auction.common.exception.InvalidBidException;
import com.auction.server.dao.AutoBidProfileDao;
import com.auction.server.dao.BidDao;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.jdbc.JdbcAutoBidProfileDao;
import com.auction.server.dao.jdbc.JdbcBidDao;
import com.auction.server.dao.jdbc.JdbcUserDao;
import com.auction.server.model.AutoBidProfile;
public class AutoBidService {
    private final AutoBidProfileDao autoBidProfileDao;
    private final BidDao bidDao;
    private final UserDao userDao;
    private final AuctionService auctionService;

    public AutoBidService(AutoBidProfileDao autoBidProfileDao,
                          BidDao bidDao,
                          UserDao userDao,
                          AuctionService auctionService) {
        this.autoBidProfileDao = autoBidProfileDao;
        this.bidDao            = bidDao;
        this.userDao           = userDao;
        this.auctionService    = auctionService;
    }

    public AutoBidService(AuctionService auctionService) {
        this(new JdbcAutoBidProfileDao(), new JdbcBidDao(), new JdbcUserDao(), auctionService);
    }

    public AutoBidProfile registerAutoBid(long userId, long auctionId, BigDecimal maxBid, BigDecimal increment) throws AuctionConnectException {
        if (maxBid == null || maxBid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max bid must be positive");
        }
        if (increment == null || increment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Increment must be positive");
        }

        //Kiểm tra xem user có từng cài auto-bid cho phiên đấu này chưa
        Optional<AutoBidProfile> existing = autoBidProfileDao.findByUserIdAndAuctionId(userId, auctionId);
        //Nếu đã cài -> cập nhật maxBid và increment
        if (existing.isPresent()) {
            AutoBidProfile profile = existing.get();
            profile.setMax_bid(maxBid);
            profile.setIncrement(increment);
            return autoBidProfileDao.update(profile);
        } else {    //Nếu chưa cài -> tạo mới
            AutoBidProfile profile = new AutoBidProfile();
            profile.setUser_id(userId);
            profile.setAuction_id(auctionId);
            profile.setMax_bid(maxBid);
            profile.setIncrement(increment);
            profile.setCreated_at(java.time.LocalDateTime.now());
            return autoBidProfileDao.save(profile);
        }
    }

    public boolean cancelAutoBid(long userId, long auctionId) {
        Optional<AutoBidProfile> profile = autoBidProfileDao.findByUserIdAndAuctionId(userId, auctionId);
        if (profile.isEmpty()) {
            return false; // Không tìm thấy profile để hủy
        }
        return autoBidProfileDao.deleteById(profile.get().getId());
    }

    //Hàm lấy thông tin AutoBidProfile
    public Optional<AutoBidProfile> getAutoBidProfile(long userId, long auctionId) {
        return autoBidProfileDao.findByUserIdAndAuctionId(userId, auctionId);
    }

    // Ham đặt giá tự động cho một phiên đấu giá dựa trên profile auto-bid của người dùng
    public boolean placeBidAutomatically(long auctionId, long userId, BigDecimal bidAmount) {
        try {
            auctionService.placeBid(auctionId, userId, bidAmount);
            return true;
        } catch (AuctionTimeException e) {
            throw new RuntimeException("[AutoBid] Auction " + auctionId + " is not running.", e);
        } catch (AuctionConnectException e) {  
            throw new RuntimeException("[AutoBid] Fail to conect with Database.", e);
        } catch (AuctionMisMatchException e) {
            throw new RuntimeException("[AutoBid] Auction ID mismatch for auction " + auctionId + ".", e);
        } catch (InvalidBidException e) {
            System.err.println("[Auction] Invalid bid amount. Skipping user " + userId);
            return false;
        }
    }
    public void processAutoBids(long auctionId, BigDecimal currentPrice, long currentBidderId) {
        //Lấy tất cả profile auto-bid của phiên đấu này
        List<AutoBidProfile> profiles = autoBidProfileDao.findByAuctionId(auctionId);
        BigDecimal currentHighestBid = currentPrice;  //Giá sàn hiện tại
        long currentHighestBidder = currentBidderId;  //ID người đang dẫn đầu hiện tại
        while (true) { 
            boolean isPriceUpdated = false;
            for (AutoBidProfile profile : profiles) {
                //Nếu người dùng này đang dẫn đầu -> bỏ qua
                if (profile.getUser_id() == currentHighestBidder) {
                    continue;
                }
                //Tính giá tiếp theo mà profile này sẽ đặt
                BigDecimal nextBid = currentHighestBid.add(profile.getIncrement());
                //Nếu giá tiếp theo vượt quá maxBid của profile -> không thể tham gia vòng này
                if (nextBid.compareTo(profile.getMax_bid()) > 0) {
                    continue;
                }
                boolean bidSuccessed = placeBidAutomatically(auctionId, profile.getUser_id(), nextBid);
                if (!bidSuccessed) {
                    continue; // Nếu đặt giá thất bại -> bỏ qua profile này
                }
                //Nếu đặt giá thành công -> cập nhật currentHighestBid và currentHighestBidder
                currentHighestBid = nextBid;
                currentHighestBidder = profile.getUser_id();
                isPriceUpdated = true;
                System.out.println("[AutoBid] User " + profile.getUser_id() + " placed an automatic bid of " + nextBid + " on auction " + auctionId);
                break; // Sau khi có một profile đặt giá thành công -> dừng vòng này để kiểm tra lại từ đầu với giá mới
            }
            //Nếu sau khi duyệt hết tất cả profile mà không có ai đặt giá thành công -> dừng vòng lặp
            if (!isPriceUpdated) {
                break;
            }
        }
    }
}
