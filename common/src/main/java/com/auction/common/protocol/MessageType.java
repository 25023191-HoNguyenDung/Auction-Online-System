package com.auction.common.protocol;

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
    REGISTER_REQ,   // thêm
    REGISTER_RES,   // thêm
}