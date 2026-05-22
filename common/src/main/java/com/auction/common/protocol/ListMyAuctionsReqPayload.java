package com.auction.common.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ListMyAuctionsReqPayload {
    private final long sellerId;

    @JsonCreator
    public ListMyAuctionsReqPayload(@JsonProperty("sellerId") long sellerId) {
        this.sellerId = sellerId;
    }

    public long getSellerId() { return sellerId; }
}
