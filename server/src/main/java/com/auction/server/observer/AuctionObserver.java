package com.auction.server.observer;

public interface AuctionObserver {
    // được gọi khi có event mới trong phiên đgia
    void onAuctionEvent(AuctionEvent event);
}
