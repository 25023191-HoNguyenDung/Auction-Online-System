package com.auction.common.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterResPayload {
    private final boolean success;
    private final String message;

    @JsonCreator
    public RegisterResPayload(
            @JsonProperty("success") boolean success,
            @JsonProperty("message") String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
