package com.auction.server.dao;

import com.auction.common.protocol.BidUpdatedEventPayload;
import com.auction.server.model.BidTransaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidDao {
    void save(BidTransaction bid);
    Optional<BidTransaction> findById(UUID bidId);
    List<BidTransaction> findByAuctionId(UUID auctionId);           // lịch sử bid của 1 phiên
    List<BidTransaction> findByBidderId(UUID bidderId);             // lịch sử bid của 1 user
    Optional<BidTransaction> findHighestBidByAuctionId(UUID auctionId); // giá cao nhất hiện tại
}
