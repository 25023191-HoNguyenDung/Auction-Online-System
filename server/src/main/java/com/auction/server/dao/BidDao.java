package com.auction.server.dao;

import com.auction.server.model.BidTransaction;
import java.util.List;
import java.util.Optional;

public interface BidDao {
    Optional<BidTransaction> findById(long id);
    /** Lịch sử bid của 1 phiên, sắp xếp theo bid_time tăng dần */
    List<BidTransaction> findByAuctionId(long auctionId);
    /** Bid có amount cao nhất; nếu bằng nhau ưu tiên bid sớm hơn */
    Optional<BidTransaction> findHighestBidByAuctionId(long auctionId);
    /** Bid không được sửa/xóa sau khi tạo */
    BidTransaction save(BidTransaction bid);
}
