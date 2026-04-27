# API Contract v1.0

## Envelope

All request/response/event messages use a shared envelope:

```json
{
  "protocolVersion": "1.0",
  "messageId": "uuid",
  "type": "PLACE_BID_REQ",
  "timestamp": "2026-04-23T10:00:00Z",
  "correlationId": "uuid-or-null",
  "payload": {}
}
```

## Message Types

- `LOGIN_REQ`, `LOGIN_RES`
- `LIST_AUCTIONS_REQ`, `LIST_AUCTIONS_RES`
- `GET_AUCTION_DETAIL_REQ`, `GET_AUCTION_DETAIL_RES`
- `PLACE_BID_REQ`, `PLACE_BID_RES`
- `SUBSCRIBE_AUCTION_REQ`, `SUBSCRIBE_AUCTION_RES`
- `UNSUBSCRIBE_AUCTION_REQ`, `UNSUBSCRIBE_AUCTION_RES`
- `BID_UPDATED_EVENT`
- `AUCTION_CLOSED_EVENT`
- `ERROR_RES`

## Payload Samples

### PLACE_BID_REQ

```json
{
  "auctionId": "uuid",
  "bidderId": "uuid",
  "amount": 1250000
}
```

### PLACE_BID_RES

```json
{
  "accepted": true,
  "auctionId": "uuid",
  "currentHighestBid": 1250000,
  "leaderBidderId": "uuid"
}
```

### BID_UPDATED_EVENT

```json
{
  "auctionId": "uuid",
  "newHighestBid": 1250000,
  "leaderBidderId": "uuid",
  "bidTime": "2026-04-23T10:01:00Z"
}
```

### ERROR_RES

```json
{
  "code": "BID_TOO_LOW",
  "message": "Bid must be higher than current highest bid",
  "details": {
    "currentHighestBid": 1200000
  }
}
```

## Error Codes

- `AUTH_INVALID_CREDENTIALS`
- `AUCTION_NOT_FOUND`
- `AUCTION_CLOSED`
- `BID_TOO_LOW`
- `FORBIDDEN_ROLE`
- `UNSUPPORTED_PROTOCOL`
- `INVALID_MESSAGE`
- `INTERNAL_ERROR`

## Core Validation

- `amount` must be strictly higher than current highest bid.
- Bids on closed auctions are rejected.
- Unsupported `protocolVersion` major must return `ERROR_RES`.