package com.auction.common.protocol; // dữ liệu trả về từ server
import java.math.BigDecimal;
import java.util.UUID;

public record PlaceBidResPayload(
        boolean accepted,
        UUID auctionId,
        BigDecimal currentHighestBid,
        UUID leaderBidderId
) {
}
