package com.auction.common.protocol;

public class ProtocolMappingException extends RuntimeException { //tạo lỗi riêng cho việc xử lý message
    public ProtocolMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
