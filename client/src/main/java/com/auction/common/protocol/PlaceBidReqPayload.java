package com.auction.common.protocol;
// dữ liệu cụ thể cho hoạt động đấu giá
import java.math.BigDecimal;// dữ liệu dùng cho tiền
import java.util.UUID; // ID duy nhất, không bị trùng

public record PlaceBidReqPayload(UUID auctionId, UUID bidderId, BigDecimal amount) {
}