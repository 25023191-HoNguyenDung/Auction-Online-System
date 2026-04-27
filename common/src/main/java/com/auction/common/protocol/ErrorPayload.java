package com.auction.common.protocol;
// dữ liệu trả về khi có lỗi
import java.util.Map;

public class ErrorPayload {
    private ErrorCode code; // mã lỗi
    private String message; // thông báo lỗi
    private Map<String, Object> details; // thông tin dạng lỗi

    public ErrorPayload() {
    }

    public ErrorPayload(ErrorCode code, String message, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public ErrorCode getCode() {
        return code;
    }

    public void setCode(ErrorCode code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
