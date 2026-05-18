package com.auction.server.dao;

import com.auction.server.model.AutoBidProfile;
import java.util.List;
import java.util.Optional;
// qua lí autobid
public interface AutoBidProfileDao {
    Optional<AutoBidProfile> findById(long id);
    List<AutoBidProfile> findByAuctionId(long auctionId);
    Optional<AutoBidProfile> findByUserIdAndAuctionId(long userId, long auctionId); // ktra user này có đag bật autobid ko
    AutoBidProfile save(AutoBidProfile profile);
    AutoBidProfile update(AutoBidProfile profile);
    boolean deleteById(long id);
}
