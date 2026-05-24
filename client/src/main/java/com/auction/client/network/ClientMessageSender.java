package com.auction.client.network;

import com.auction.common.protocol.*;

import java.math.BigDecimal;

/**
 * PHIÊN BẢN ĐÃ SỬA:
 * - Thêm sendDeposit() để gửi yêu cầu nạp tiền lên server
 *
 * PATH: client/src/main/java/com/auction/client/network/ClientMessageSender.java
 */
public class ClientMessageSender {
    private final ServerConnection connection;
    private final ProtocolMapper mapper;

    public ClientMessageSender() {
        this.connection = ServerConnection.getInstance();
        this.mapper = new ProtocolMapper();
    }

    // Gửi yêu cầu đăng nhập
    public String sendLogin(String username, String password) {
        LoginReqPayload payload = new LoginReqPayload(username, password);
        MessageEnvelope envelope = mapper.buildRequest(MessageType.LOGIN_REQ, payload);
        send(envelope);
        return envelope.getMessageId();
    }

    // Gửi yêu cầu lấy danh sách phiên đấu giá
    public String sendListAuctions(long userId, int page, int size, String statusFilter) {
        ListAuctionsReqPayload payload = new ListAuctionsReqPayload(userId, page, size, statusFilter);
        MessageEnvelope envelope = mapper.buildRequest(MessageType.LIST_AUCTIONS_REQ, payload);
        send(envelope);
        return envelope.getMessageId();
    }

    // Gửi yêu cầu đặt giá
    public String sendPlaceBid(long auctionId, long bidderId, BigDecimal amount) {
        PlaceBidReqPayload payload = new PlaceBidReqPayload(auctionId, bidderId, amount);
        MessageEnvelope envelope = mapper.buildRequest(MessageType.PLACE_BID_REQ, payload);
        send(envelope);
        return envelope.getMessageId();
    }

    // ← THÊM MỚI: Gửi yêu cầu nạp tiền lên server
    public String sendDeposit(long userId, BigDecimal amount) {
        DepositReqPayload payload = new DepositReqPayload(userId, amount);
        MessageEnvelope envelope = mapper.buildRequest(MessageType.DEPOSIT_REQ, payload);
        send(envelope);
        return envelope.getMessageId();
    }

    // Gửi yêu cầu đăng ký
    public String sendRegister(String username, String email, String password, String role) {
        RegisterReqPayload payload = new RegisterReqPayload(username, email, password, role);
        MessageEnvelope envelope = mapper.buildRequest(MessageType.REGISTER_REQ, payload);
        send(envelope);
        return envelope.getMessageId();
    }

    // Gửi mọi loại message
    private void send(MessageEnvelope envelope) {
        String json = mapper.toJson(envelope);
        synchronized (connection.getOut()) {
            connection.getOut().println(json);
            connection.getOut().flush();
        }
    }
}