# Architecture — Online Auction System

## Tổng quan kiến trúc
bản thiết kế tổng thể của hệ thống

Hệ thống theo mô hình **Client–Server** với giao tiếp qua **TCP Socket + JSON**.

```
┌─────────────────────────────────────────────────────────┐
│                        CLIENT                           │
│  JavaFX UI ──► Controller ──► ClientMessageSender       │
│                               ServerEventListener ◄──  │
│                               ServerConnection          │
└────────────────────────────┬────────────────────────────┘
                             │ TCP Socket (port 1337)
                             │ JSON messages
┌────────────────────────────▼────────────────────────────┐
│                        SERVER                           │
│  AuctionServer                                          │
│    └── ClientConnectionHandler (1 thread/client)        │
│          └── RequestDispatcher                          │
│                ├── AuthController                       │
│                ├── AuctionController                    │
│                └── BidController                        │
│                      └── AuctionService                 │
│                            ├── AuctionLogicManager      │
│                            ├── AutoBidService           │
│                            └── AuctionClosingService    │
│                                  └── DAO Layer          │
│                                        └── MySQL DB     │
└─────────────────────────────────────────────────────────┘
```

---

## Tầng Protocol (common)

Dùng chung giữa client và server.

```
MessageEnvelope
  ├── protocolVersion: "1.0"
  ├── messageId: UUID
  ├── type: MessageType
  ├── timestamp: ISO-8601
  ├── correlationId: UUID (null nếu là request/event)
  └── payload: JsonNode

MessageType:
  REQUEST : LOGIN_REQ, LIST_AUCTIONS_REQ, PLACE_BID_REQ
  RESPONSE: LOGIN_RES, LIST_AUCTIONS_RES, PLACE_BID_RES, ERROR_RES
  EVENT   : BID_UPDATED_EVENT, AUCTION_CLOSED_EVENT
```

---

## Tầng Network (server)

```
AuctionServer (port 1337)
  │  accept() → tạo thread mới cho mỗi client
  └── ClientConnectionHandler (Runnable, 1 thread/client)
        │  readLine() → parseEnvelope() → dispatch()
        └── RequestDispatcher
              │  switch(MessageType)
              ├── LOGIN_REQ         → UserDao.findByUsername()
              ├── LIST_AUCTIONS_REQ → AuctionService.getAllAuctions()
              └── PLACE_BID_REQ     → AuctionService.placeBid()
                                       → AuctionEventPublisher.publish()
```

---

## Tầng Service (server)

```
AuctionService (Interface)
  └── AuctionServiceImpl
        ├── AuctionLogicManager  ← logic nghiệp vụ + ReadWriteLock
        ├── AutoBidService       ← tự động đặt giá thay user
        └── AuctionClosingService ← scheduler đóng phiên hết giờ
```

---

## Tầng Observer — Realtime Update

```
placeBid() thành công
  → AuctionEventPublisher.publish(BID_PLACED event)
      → ClientAuctionObserver.onAuctionEvent()
          → mapper.toJson(BID_UPDATED_EVENT)
              → socketOut.println(json)
                  → ServerEventListener (client) nhận
                      → setOnBidUpdated handler
                          → JavaFX UI cập nhật realtime
```

---

## Tầng DAO (server)

```
Interface          Implementation
─────────────────────────────────
UserDao        →   JdbcUserDao
ItemDao        →   JdbcItemDao
AuctionDao     →   JdbcAuctionDao
BidDao         →   JdbcBidDao
AutoBidProfileDao → JdbcAutoBidProfileDao
```

---

## Design Patterns áp dụng

| Pattern | Áp dụng ở |
|---------|-----------|
| Singleton | `DatabaseConfig`, `AuctionLockManager`, `AuctionEventPublisher`, `SubscriptionRegistry`, `ServerConnection` |
| Factory Method | `ItemFactory` — tạo `Electronics`, `Art`, `Vehicle` |
| Observer | `AuctionEventPublisher` + `ClientAuctionObserver` — realtime update |
| Strategy | `AuctionLogicManager` — xử lý bid logic |
| DAO | Tách biệt tầng data access khỏi business logic |

---

## Concurrency

| Cơ chế | Mục đích |
|--------|----------|
| `ReentrantReadWriteLock` trong `AuctionLogicManager` | Tránh race condition khi nhiều người đặt giá cùng lúc |
| `ConcurrentHashMap` trong `AuctionEventPublisher` | Thread-safe subscribe/unsubscribe |
| `CopyOnWriteArrayList` | Thread-safe duyệt observer list khi broadcast |
| `ScheduledExecutorService` trong `AuctionClosingService` | Tự động đóng phiên hết giờ mỗi 10 giây |
| `ExecutorService` (ThreadPool 50) trong `AuctionServer` | Xử lý tối đa 50 client đồng thời |