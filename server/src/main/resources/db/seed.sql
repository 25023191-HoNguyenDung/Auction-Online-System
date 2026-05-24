SET SQL_SAFE_UPDATES = 0;
USE auction_db;

-- Xóa dữ liệu cũ trước khi insert (an toàn)
DELETE FROM bids;
DELETE FROM auto_bid_profiles;
DELETE FROM auctions;
DELETE FROM items;
DELETE FROM users;

-- 1. Users
INSERT INTO users (id, user_name, password, email, role,account_balance) VALUES
(1, 'admin', '84acb98c6f06cfe0a314ddb2f3350f7aecfccf060ae8b1e5e434555f80844579', 'admin@gmail.com', 'ADMIN',NULL),
(2, 'seller1', '1b20b0d6f803116d508571867aaa197e1d3569f7010bddf4ff4ae607c4c27011', 'ducanh@gmail.com', 'SELLER',4000000.0000),
(3, 'bidder1', 'e110381de23b574ba7c8c1452fdb2838da7959ba83cdfc5adf0391e57f44b510', 'dung@gmail.com',  'BIDDER',5000000.0000),
(4, 'bidder2', 'd93094701724dd141c2f6f81f67dff9883da420f72faa970a7e62fcf1b6d028f', 'khanh@gmail.com',  'BIDDER',3000000.0000);

-- 2. Items
INSERT INTO items (id, seller_id, name, description, category, starting_price, current_price, image_url) VALUES
(1, 2, 'iphone 17 pro max',         'new 100%',              'ELECTRONICS', 25000000.00,  25000000.00,  NULL),
(2, 2, 'tranh sơn dầu phong cảnh',  'nguyên bản thế kỷ 19', 'ART',          8000000.00,   8000000.00,  NULL),
(3, 2, 'SH 150i',                   'xe nhập khẩu',          'VEHICLE',    145000000.00, 145000000.00,  NULL);
-- 3. Auctions
INSERT INTO auctions (id, item_id, seller_id, starting_price, current_price, status, start_time, end_time, winner_bidder_id) VALUES
(1, 1, 2, 25000000.00, 28500000.00, 'RUNNING', NOW(), DATE_ADD(NOW(), INTERVAL 2 DAY), NULL),
(2, 2, 2, 8000000.00, 8000000.00, 'OPEN', DATE_ADD(NOW(), INTERVAL 8 HOUR), DATE_ADD(NOW(), INTERVAL 4 DAY), NULL),
(3, 3, 2, 145000000.00, 162000000.00, 'FINISHED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 3);

-- 4. Auto Bid Profiles
INSERT INTO auto_bid_profiles (id, user_id, auction_id, max_bid, increment) VALUES
(1, 3, 1, 35000000.00, 500000.00),
(2, 4, 1, 32000000.00, 1000000.00);

-- 5. Bids
INSERT INTO bids (bidId, auctionId, bidder, amount, bid_time) VALUES
(1, 1, 3, 26000000.00, NOW()),
(2, 1, 4, 27000000.00, NOW()),
(3, 1, 3, 28000000.00, NOW()),
(4, 1, 4, 28500000.00, NOW());