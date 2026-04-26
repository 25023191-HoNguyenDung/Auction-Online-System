package com.auction.common.protocol;
// chuyển qua lại từ JSON sang object server
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ProtocolMapper {
    public static final String PROTOCOL_VERSION = "1.0";

    private final ObjectMapper objectMapper;

    public ProtocolMapper() {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public ProtocolMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MessageEnvelope parseEnvelope(String rawJson) {
        try {
            return objectMapper.readValue(rawJson, MessageEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new ProtocolMappingException("Failed to parse message envelope", e);
        }
    }

    public <T> T parsePayload(MessageEnvelope envelope, Class<T> payloadType) {
        try {
            return objectMapper.treeToValue(envelope.payload(), payloadType);
        } catch (JsonProcessingException e) {
            throw new ProtocolMappingException(
                    "Failed to parse payload for type: " + envelope.type(),
                    e
            );
        }
    }

    public String toJson(MessageEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new ProtocolMappingException("Failed to serialize message envelope", e);
        }
    }

    public MessageEnvelope buildRequest(MessageType type, Object payload) {
        return new MessageEnvelope(
                PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                type,
                Instant.now().toString(),
                null,
                objectMapper.valueToTree(payload)
        );
    }

    public MessageEnvelope buildResponse(MessageType type, String correlationId, Object payload) {
        return new MessageEnvelope(
                PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                type,
                Instant.now().toString(),
                correlationId,
                objectMapper.valueToTree(payload)
        );
    }

    public MessageEnvelope buildEvent(MessageType type, Object payload) {
        return new MessageEnvelope(
                PROTOCOL_VERSION,
                UUID.randomUUID().toString(),
                type,
                Instant.now().toString(),
                null,
                objectMapper.valueToTree(payload)
        );
    }

    public MessageEnvelope buildErrorResponse(
            String correlationId,
            ErrorCode code,
            String message,
            JsonNode details
    ) {
        ErrorPayload payload = new ErrorPayload(code, message, objectMapper.convertValue(details, objectMapper.getTypeFactory().constructMapType(java.util.Map.class, String.class, Object.class)));
        return buildResponse(MessageType.ERROR_RES, correlationId, payload);
    }

    public MessageEnvelope buildErrorResponse(
            String correlationId,
            ErrorCode code,
            String message
    ) {
        ErrorPayload payload = new ErrorPayload(code, message, java.util.Map.of());
        return buildResponse(MessageType.ERROR_RES, correlationId, payload);
    }
}
