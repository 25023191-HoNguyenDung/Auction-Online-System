package com.auction.common.protocol;

import com.fasterxml.jackson.databind.JsonNode;
// đóng gói message theo khung
public class MessageEnvelope {
    private String protocolVersion; // phiên bản message
    private String messageId;
    private MessageType type;
    private String timestamp; // thời gian gửi message
    private String correlationId; // id liên kết các message
    private JsonNode payload; // nội dung

    public MessageEnvelope() {
    }

    public MessageEnvelope(
            String protocolVersion,
            String messageId,
            MessageType type,
            String timestamp,
            String correlationId,
            JsonNode payload
    ) {
        this.protocolVersion = protocolVersion;
        this.messageId = messageId;
        this.type = type;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
        this.payload = payload;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}
