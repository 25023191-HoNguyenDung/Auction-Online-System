package com.auction.server.dao;

import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionDao {
    void save(Auction auction);                          // thêm mới
    void update(Auction auction);                        // cập nhật
    void delete(UUID auctionId);                         // xóa
    Optional<Auction> findById(UUID auctionId);          // tìm theo id
    List<Auction> findAll();                             // lấy tất cả
    List<Auction> findByStatus(AuctionStatus status);   // lọc theo trạng thái
    List<Auction> findBySellerId(UUID sellerId);        // lọc theo seller
    List<Auction> findPage(int offset, int size, AuctionStatus statusFilter); // lấy ds auction theo trang
    int countByStatus(AuctionStatus status);            // đếm số auction theo trạng thái

}
