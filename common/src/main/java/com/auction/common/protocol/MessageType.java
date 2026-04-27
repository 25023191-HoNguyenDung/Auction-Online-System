package com.auction.common.protocol;
// ds các loại message
public enum MessageType {
    PLACE_BID_REQ, // đặt giá
    PLACE_BID_RES, // trả về request
    BID_UPDATED_EVENT, // thông báo mn có giá mới
    ERROR_RES // có lỗi
}
