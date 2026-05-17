package com.auction.common.protocol;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

// test ProtocolMapper
class ProtocolMapperTest {

    private final ProtocolMapper mapper = new ProtocolMapper();

    @Test
    void testPlaceBidRequestRoundTrip() { //Object → JSON → Object xem dữ liệu có dữ nguyên không
        PlaceBidReqPayload payload = new PlaceBidReqPayload(
                ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE), // ramdom cho đa luồng
                ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE),
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
    void testBuildResponseHasCorrelationId() { //Test xem dữ liệu server trả về có đầy đủ thông tin cần thiết để client hiểu và ghép với request ban đầu không
        String correlationId = UUID.randomUUID().toString();

        PlaceBidResPayload payload = new PlaceBidResPayload(
                true,
                ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE),
                new BigDecimal("1500000"),
                ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE)
        );

        MessageEnvelope res = mapper.buildResponse(MessageType.PLACE_BID_RES, correlationId, payload);

        assertEquals(MessageType.PLACE_BID_RES, res.getType());
        assertEquals(correlationId, res.getCorrelationId());
        assertNotNull(res.getMessageId());
        assertNotNull(res.getTimestamp());
    }

    @Test
    void testBidUpdatedEventRoundTrip() { //Test xem event gửi đi → chuyển thành JSON → parse lại có giữ nguyên dữ liệu không
        BidUpdatedEventPayload payload = new BidUpdatedEventPayload(
                ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE),
                new BigDecimal("1700000"),
                ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE),
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
    void testInvalidJsonThrowsException() { //Đưa JSON sai → phải ném ra exception
        assertThrows(ProtocolMappingException.class, () -> mapper.parseEnvelope("{invalid-json"));
    }
}