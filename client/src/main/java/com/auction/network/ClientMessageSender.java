package com.auction.network;

import com.auction.common.protocol.*;

import java.math.BigDecimal;
import java.util.UUID;
// gói req gửi lên server
public class ClientMessageSender {
    private final ServerConnection connection;
    private final ProtocolMapper mapper;

    public ClientMessageSender(){
        this.connection = ServerConnection.getInstance();
        this.mapper = new ProtocolMapper();
    }

    // gửi yc đăng nhập
    public String sendLogin(String username, String password){
        LoginReqPayload payload = new LoginReqPayload(username,password); // tạo payload
        MessageEnvelope envelope = mapper.buildRequest(MessageType.LIST_AUCTIONS_REQ,payload); // đóng thành message
        send(envelope);
        return envelope.getMessageId(); // để ghép với res tương ứng
    }

    // gửi yc lấy ds phiên đgia
    public String sendListAuctions(long userId, int page, int size, String statusFilter) {
        ListAuctionsReqPayload payload = new ListAuctionsReqPayload(userId, page, size, statusFilter); //tạo payload
        MessageEnvelope envelope = mapper.buildRequest(MessageType.LIST_AUCTIONS_REQ, payload); // đóng thành mess
        send(envelope);
        return envelope.getMessageId();
    }

    public String sendPlaceBid(long auctionId, long bidderId, BigDecimal amount) {
        PlaceBidReqPayload payload = new PlaceBidReqPayload(auctionId, bidderId, amount); // tạo payload
        MessageEnvelope envelope = mapper.buildRequest(MessageType.PLACE_BID_REQ, payload); // đóng thành mess
        send(envelope);
        return envelope.getMessageId();
    }

    // gửi mọi loại mess
    private void send(MessageEnvelope envelope) {
        String json = mapper.toJson(envelope); // chuyển sang JSON
        synchronized (connection.getOut()) {
            connection.getOut().println(json); // ghi dlieu xuống socket
            connection.getOut().flush(); // buộc gửi ngay
        }
    }
    
}
