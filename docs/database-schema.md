
# Database Schema — Online Auction System

## Tổng quan
bản thiết kế cơ sở dữ liệu

Hệ thống sử dụng MySQL với 5 bảng chính.

```
users ──< items ──< auctions ──< bids
  │                    │
  └──────────────────< auto_bid_profiles
```

---

## Bảng `users`

Lưu thông tin tất cả người dùng (Bidder, Seller, Admin).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID người dùng |
| `user_name` | VARCHAR(50) | NOT NULL, UNIQUE | Tên đăng nhập |
| `password` | VARCHAR(300) | NOT NULL | Mật khẩu (hash) |
| `email` | VARCHAR(100) | NOT NULL, UNIQUE | Email |
| `role` | VARCHAR(20) | NOT NULL | BIDDER / SELLER / ADMIN |

---

## Bảng `items`

Lưu thông tin sản phẩm đấu giá.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID sản phẩm |
| `seller_id` | BIGINT | FK → users.id | Người bán |
| `name` | VARCHAR(200) | NOT NULL | Tên sản phẩm |
| `description` | TEXT | | Mô tả |
| `category` | VARCHAR(50) | | ELECTRONICS / ART / VEHICLE |
| `starting_price` | DECIMAL(15,2) | NOT NULL | Giá khởi điểm |
| `current_price` | DECIMAL(15,2) | NOT NULL | Giá hiện tại |
| `image_url` | VARCHAR(500) | | Ảnh sản phẩm |
| `reserve_price` | DECIMAL(15,2) | | Giá sàn |

---

## Bảng `auctions`

Lưu thông tin phiên đấu giá.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID phiên |
| `item_id` | BIGINT | FK → items.id | Sản phẩm |
| `seller_id` | BIGINT | FK → users.id | Người bán |
| `starting_price` | DECIMAL(15,2) | NOT NULL | Giá khởi điểm |
| `current_price` | DECIMAL(15,2) | NOT NULL | Giá hiện tại |
| `status` | ENUM | NOT NULL | OPEN / RUNNING / FINISHED / PAID / CANCELED |
| `start_time` | DATETIME | NOT NULL | Thời gian bắt đầu |
| `end_time` | DATETIME | NOT NULL | Thời gian kết thúc |
| `winner_bidder_id` | BIGINT | FK → users.id, NULL | Người thắng |

**Luồng trạng thái:**
```
OPEN → RUNNING → FINISHED → PAID
                          → CANCELED
```

---

## Bảng `bids`

Lưu lịch sử đặt giá (bất biến, không sửa/xóa).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `bidId` | BIGINT | PK, AUTO_INCREMENT | ID bid |
| `auctionId` | BIGINT | FK → auctions.id | Phiên đấu giá |
| `bidder` | BIGINT | FK → users.id | Người đặt giá |
| `amount` | DECIMAL(15,2) | NOT NULL | Số tiền đặt |
| `bid_time` | TIMESTAMP | DEFAULT NOW() | Thời điểm đặt |

---

## Bảng `auto_bid_profiles`

Lưu cấu hình đấu giá tự động.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID profile |
| `user_id` | BIGINT | FK → users.id | Người dùng |
| `auction_id` | BIGINT | FK → auctions.id | Phiên đấu giá |
| `max_bid` | DECIMAL(15,2) | NOT NULL | Giá tối đa chấp nhận |
| `increment` | DECIMAL(15,2) | NOT NULL | Bước tăng giá mỗi lần |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Thời điểm đăng ký |

**Ràng buộc:** `UNIQUE(user_id, auction_id)` — mỗi user chỉ có 1 profile trên 1 phiên.