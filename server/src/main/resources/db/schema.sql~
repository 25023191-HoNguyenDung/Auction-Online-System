DROP DATABASE IF EXISTS auction_db;
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auction_db;


CREATE TABLE `users` (
`id` bigint NOT NULL AUTO_INCREMENT,
`user_name` varchar(50) NOT NULL,
`password` varchar(300) NOT NULL,
`email` varchar(100) NOT NULL,
`role` varchar(20) NOT NULL,
PRIMARY KEY (`id`),
 UNIQUE KEY `username` (`user_name`),
 UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `items` (
`id`  bigint NOT NULL AUTO_INCREMENT,
`seller_id` bigint NOT NULL,
`name` varchar(200) NOT NULL,
`description` text,
 `category` varchar(50) DEFAULT NULL,
`starting_price` decimal(15,2) NOT NULL,
`current_price` decimal(15,2) NOT NULL,       -- thêm: giá hiện tại
`image_url`  varchar(500)  DEFAULT NULL,   -- thêm: ảnh sản phẩm
`reserve_price` decimal(15,2) NULL DEFAULT NULL, -- thêm: giá sàn
PRIMARY KEY (`id`),
KEY `seller_id` (`seller_id`),
CONSTRAINT `items_ibfk_1` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `auctions` (
`id` bigint NOT NULL AUTO_INCREMENT,
`item_id` bigint NOT NULL,
`seller_id` bigint NOT NULL,
`starting_price` decimal(15,2) NOT NULL,
`current_price` decimal(15,2) NOT NULL,
`status` enum('OPEN','RUNNING','FINISHED','PAID','CANCELED') NOT NULL DEFAULT 'OPEN',
`start_time` datetime NOT NULL,
`end_time` datetime NOT NULL,
`winner_bidder_id` bigint DEFAULT NULL,
PRIMARY KEY (`id`),
KEY `item_id` (`item_id`),
KEY `seller_id` (`seller_id`),
KEY `winner_bidder_id` (`winner_bidder_id`),
CONSTRAINT `auctions_ibfk_1` FOREIGN KEY (`item_id`) REFERENCES `items` (`id`) ON DELETE CASCADE,
CONSTRAINT `auctions_ibfk_2` FOREIGN KEY (`seller_id`) REFERENCES `users` (`id`),
CONSTRAINT `auctions_ibfk_3` FOREIGN KEY (`winner_bidder_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `bids` (
`bidId` bigint NOT NULL AUTO_INCREMENT,
`auctionId` bigint NOT NULL,
`bidder` bigint NOT NULL,
`amount` decimal(15,2) NOT NULL,
`bid_time` timestamp DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`bidId`),
KEY `auction_id` (`auctionId`),
KEY `bidder_id` (`bidder`),
CONSTRAINT `bids_ibfk_1` FOREIGN KEY (`auctionId`) REFERENCES `auctions` (`id`) ON DELETE CASCADE,
CONSTRAINT `bids_ibfk_2` FOREIGN KEY (`bidder`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `auto_bid_profiles` (
`id` bigint NOT NULL AUTO_INCREMENT,
`user_id` bigint NOT NULL,
`auction_id` bigint NOT NULL,
`max_bid` decimal(15,2) NOT NULL,
`increment` decimal(15,2) NOT NULL,
`created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE KEY `unique_auto_bid` (`user_id`,`auction_id`),
KEY `auction_id` (`auction_id`),
CONSTRAINT `auto_bid_profiles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
CONSTRAINT `auto_bid_profiles_ibfk_2` FOREIGN KEY (`auction_id`) REFERENCES `auctions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;