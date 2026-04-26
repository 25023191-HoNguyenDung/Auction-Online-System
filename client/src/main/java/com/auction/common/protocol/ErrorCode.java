package com.auction.common.protocol;
// ds lỗi trả về
public enum ErrorCode {
    AUTH_INVALID_CREDENTIALS, // sai tk/mk
    AUCTION_NOT_FOUND, // không tìm thấy phiên đấu giá
    AUCTION_CLOSED, // phiên đóng ko cho đặt giá nữa
    BID_TOO_LOW, // giá đặt <= hiện tại
    FORBIDDEN_ROLE, // ko có quyền làm hành động này
    UNSUPPORTED_PROTOCOL, // version protocol ko đc hỗ trợ
    INVALID_MESSAGE, // message sai format ...
    INTERNAL_ERROR // exception không mong muốn
}
