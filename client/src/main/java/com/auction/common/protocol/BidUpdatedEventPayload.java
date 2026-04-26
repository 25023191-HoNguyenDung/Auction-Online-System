package com.auction.common.protocol;
//
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BidUpdatedEventPayload(
        UUID auctionId,
        BigDecimal newHighestBid,
        UUID leaderBidderId,
        Instant bidTime
) {
}
