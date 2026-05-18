package com.auction.server.dao;

import com.auction.server.model.BidTransaction;
import java.util.List;
import java.util.Optional;
// thao tác với dữ liệu lq đến đặt giá
public interface BidDao {
    Optional<BidTransaction> findById(long id);
    List<BidTransaction> findByAuctionId(long auctionId);
    Optional<BidTransaction> findHighestBidByAuctionId(long auctionId);
    BidTransaction save(BidTransaction bid);
}
