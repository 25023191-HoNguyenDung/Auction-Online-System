package com.auction.server.dao;

import com.auction.server.model.AutoBidProfile;
import java.util.List;
import java.util.Optional;

public interface AutoBidProfileDao {
    Optional<AutoBidProfile> findById(long id);
    /** Tất cả auto-bid profiles của 1 phiên, sắp xếp created_at ASC (ai đăng trước ưu tiên trước) */
    List<AutoBidProfile> findByAuctionId(long auctionId);
    Optional<AutoBidProfile> findByUserIdAndAuctionId(long userId, long auctionId);
    AutoBidProfile save(AutoBidProfile profile);
    AutoBidProfile update(AutoBidProfile profile);
    boolean deleteById(long id);
}
