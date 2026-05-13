package com.auction.server.observer;

import com.auction.common.protocol.*;

import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
// observer cụ thể : khi có thay đổi thì đóng gói tin nhắn gửi cho người dùng
public class ClientAuctionObserver implements AuctionObserver {
    private final String clientId;
    private final PrintWriter socketOut; // gửi dữ liệu từ server đến client
    private final ProtocolMapper mapper;

    public ClientAuctionObserver(String clientId, PrintWriter socketOut){
        this.clientId = clientId;
        this.socketOut = socketOut;
        this.mapper = new ProtocolMapper();
    }


    @Override
    // khi có event mới chuyển thành json gửi qua client
    public void onAuctionEvent(AuctionEvent event) {
        try{
            MessageEnvelope envelope = buildEnvelope(event);
            if(envelope == null) return;
            String json = mapper.toJson(envelope);
            synchronized (socketOut){
                socketOut.println(json); // gửi đến client
                socketOut.flush(); // gửi lập tức
            }
        } catch (Exception e) {
            System.err.println("[ClientAuctionObserver] Lỗi gửi event đến client "
                    + clientId + ": " + e.getMessage());
        }
    }

    // chuyển AuctionEvent thành MessageEnvelope
    private MessageEnvelope buildEnvelope(AuctionEvent event){
        switch (event.getType()) {
            case BID_PLACED:
            case PRICE_UPDATED:{
                BidUpdatedEventPayload payload = new BidUpdatedEventPayload(event.getAuctionId(),event.getCurrentPrice(),event.getBidderID(), toInstant(event.getTimestamp()));
                return mapper.buildEvent(MessageType.BID_UPDATED_EVENT,payload);
            }
            case AUCTION_CLOSED: {
                long winnerId = 0;
                if(event.getWinnerBidderId() != 0){
                    winnerId = event.getWinnerBidderId();
                }
                AuctionClosedEventPayload payload = new AuctionClosedEventPayload(event.getAuctionId(),event.getCurrentPrice(),winnerId,"FINISHED",toInstant(event.getTimestamp()));
                return mapper.buildEvent(MessageType.AUCTION_CLOSED_EVENT, payload);
            }
            case AUCTION_STARTED:
                return null;
            default:
                return null;
        }
    }

    private Instant toInstant(LocalDateTime ldt) {
        return ldt.toInstant(ZoneOffset.UTC);
    }

    public String getClientId() {
        return clientId;
    }
}
