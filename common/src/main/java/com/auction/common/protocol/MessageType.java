package com.auction.common.protocol;

/**
 * THÊM MỚI: DEPOSIT_REQ và DEPOSIT_RES
 *
 * PATH: common/src/main/java/com/auction/common/protocol/MessageType.java
 */
public enum MessageType {
    PLACE_BID_REQ,
    PLACE_BID_RES,
    BID_UPDATED_EVENT,
    ERROR_RES,
    AUCTION_CLOSED_EVENT,
    LOGIN_REQ,
    LOGIN_RES,
    LIST_AUCTIONS_REQ,
    LIST_AUCTIONS_RES,
    REGISTER_REQ,
    REGISTER_RES,
    SUBSCRIBE_REQ,
    UNSUBSCRIBE_REQ,
    DEPOSIT_REQ,   // ← THÊM MỚI
    DEPOSIT_RES    // ← THÊM MỚI
}