package com.auction.common.protocol;
// ds các loại message
public enum MessageType {
    PLACE_BID_REQ, // đặt giá
    PLACE_BID_RES, // trả về request
    BID_UPDATED_EVENT, // thông báo mn có giá mới
    ERROR_RES, // có lỗi
    AUCTION_CLOSED_EVENT, // phiên vừa kết thúc
    LOGIN_REQ, // client gửi yc đăng nhập
    LOGIN_RES, //server trả kq đăng nhập
    LIST_AUCTIONS_REQ, // client gửi yc lấy ds phiên
    LIST_AUCTIONS_RES, // server trả ds phiên
}
