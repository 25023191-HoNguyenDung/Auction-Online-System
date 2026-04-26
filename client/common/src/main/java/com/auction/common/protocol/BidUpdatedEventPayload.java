package contract_server;
// server gửi thông báo khi có giá mới
import java.math.BigDecimal;
import java.time.Instant;//thời gian hiện tại
import java.util.UUID;

public record BidUpdatedEventPayload(
        UUID auctionId,
        BigDecimal newHighestBid,
        UUID leaderBidderId,
        Instant bidTime
) {
}
