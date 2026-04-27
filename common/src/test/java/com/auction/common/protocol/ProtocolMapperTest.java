package com.auction.common.protocol;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// test ProtocolMapper
class ProtocolMapperTest {

    private final ProtocolMapper mapper = new ProtocolMapper();

    @Test
    void testPlaceBidRequestRoundTrip() {
        PlaceBidReqPayload payload = new PlaceBidReqPayload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("1250000")
        );

        MessageEnvelope req = mapper.buildRequest(MessageType.PLACE_BID_REQ, payload);
        String json = mapper.toJson(req);

        MessageEnvelope parsedEnvelope = mapper.parseEnvelope(json);
        PlaceBidReqPayload parsedPayload = mapper.parsePayload(parsedEnvelope, PlaceBidReqPayload.class);

        assertEquals(MessageType.PLACE_BID_REQ, parsedEnvelope.getType());
        assertEquals(payload.getAuctionId(), parsedPayload.getAuctionId());
        assertEquals(payload.getBidderId(), parsedPayload.getBidderId());
        assertEquals(0, payload.getAmount().compareTo(parsedPayload.getAmount()));
    }

    @Test
    void testBuildResponseHasCorrelationId() {
        String correlationId = UUID.randomUUID().toString();

        PlaceBidResPayload payload = new PlaceBidResPayload(
                true,
                UUID.randomUUID(),
                new BigDecimal("1500000"),
                UUID.randomUUID()
        );

        MessageEnvelope res = mapper.buildResponse(MessageType.PLACE_BID_RES, correlationId, payload);

        assertEquals(MessageType.PLACE_BID_RES, res.getType());
        assertEquals(correlationId, res.getCorrelationId());
        assertNotNull(res.getMessageId());
        assertNotNull(res.getTimestamp());
    }

    @Test
    void testBidUpdatedEventRoundTrip() {
        BidUpdatedEventPayload payload = new BidUpdatedEventPayload(
                UUID.randomUUID(),
                new BigDecimal("1700000"),
                UUID.randomUUID(),
                Instant.now()
        );

        MessageEnvelope event = mapper.buildEvent(MessageType.BID_UPDATED_EVENT, payload);
        String json = mapper.toJson(event);

        MessageEnvelope parsedEnvelope = mapper.parseEnvelope(json);
        BidUpdatedEventPayload parsedPayload = mapper.parsePayload(parsedEnvelope, BidUpdatedEventPayload.class);

        assertEquals(MessageType.BID_UPDATED_EVENT, parsedEnvelope.getType());
        assertEquals(payload.getAuctionId(), parsedPayload.getAuctionId());
        assertEquals(0, payload.getNewHighestBid().compareTo(parsedPayload.getNewHighestBid()));
    }

    @Test
    void testInvalidJsonThrowsException() {
        assertThrows(ProtocolMappingException.class, () -> mapper.parseEnvelope("{invalid-json"));
    }
}