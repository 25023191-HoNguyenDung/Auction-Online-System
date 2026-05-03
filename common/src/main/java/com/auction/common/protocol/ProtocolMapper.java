package com.auction.common.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
// chuyển đổi obj với JSON và đảm bảo message đúng với khuôn
public class ProtocolMapper {
    public static final String PROTOCOL_VERSION = "1.0";

    private final ObjectMapper objectMapper; // chuyển đổi giữa obj và JSON

    public ProtocolMapper() {
        this.objectMapper = new ObjectMapper().findAndRegisterModules(); // tạo obj và tự động tìm và đăng ký các module hỗ trợ thêm
    }

    public ProtocolMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    } //nhận ObjectMapper để dùng lại

    public MessageEnvelope parseEnvelope(String rawJson) { // nhận JSON -> MessageEnvelope
        try {
            return objectMapper.readValue(rawJson, MessageEnvelope.class); // đọc JSON chuyển -> MessageEnvelope
        } catch (JsonProcessingException e) {
            throw new ProtocolMappingException("Failed to parse message envelope", e);
        }
    }

    public <T> T parsePayload(MessageEnvelope envelope, Class<T> payloadType) { //payload JSON → object cụ thể
        try {
            return objectMapper.treeToValue(envelope.getPayload(), payloadType);
        } catch (JsonProcessingException e) {
            throw new ProtocolMappingException(
                    "Failed to parse payload for type: " + envelope.getType(),
                    e
            );
        }
    }

    public String toJson(MessageEnvelope envelope) { // biến MessageEnvelope -> JSON
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new ProtocolMappingException("Failed to serialize message envelope", e);
        }
    }

    public MessageEnvelope buildRequest(MessageType type, Object payload) { // lấy dữ liệu bạn muốn gửi → đóng gói thành message chuẩn để gửi đi
        return new MessageEnvelope(
                PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                type,
                Instant.now().toString(),
                null, //biết response này là trả lời cho request nào
                objectMapper.valueToTree(payload)
        );
    }

    public MessageEnvelope buildResponse(MessageType type, String correlationId, Object payload) { // tạo message trả lời request , gắn với request bđầu
        return new MessageEnvelope(
                PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                type,
                Instant.now().toString(),
                correlationId,
                objectMapper.valueToTree(payload)
        );
    }

    public MessageEnvelope buildEvent(MessageType type, Object payload) { // tạo message để thông báo cho các service khác khi có sự kiện xảy ra
        return new MessageEnvelope(
                PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                type,
                Instant.now().toString(),
                null,
                objectMapper.valueToTree(payload)
        );
    }

    public MessageEnvelope buildErrorResponse( //tạo message báo lỗi để trả về cho request
            String correlationId,
            ErrorCode code, // mã lỗi
            String message, // nội dung lỗi
            JsonNode details // thông tin chi tiết
    ) {
        Map<String, Object> detailMap;

        if (details == null) {
            detailMap = Collections.emptyMap(); // dùng {} (map rỗng)
        } else {
            detailMap = objectMapper.convertValue(
                    details,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );
        }

        ErrorPayload payload = new ErrorPayload(code, message, detailMap);
        return buildResponse(MessageType.ERROR_RES, correlationId, payload);
    }

    public MessageEnvelope buildErrorResponse( //tạo message báo lỗi đơn giản hơn không cần chi tiết
            String correlationId,
            ErrorCode code,
            String message
    ) {
        ErrorPayload payload = new ErrorPayload(code, message, Collections.emptyMap());
        return buildResponse(MessageType.ERROR_RES, correlationId, payload);
    }
}
