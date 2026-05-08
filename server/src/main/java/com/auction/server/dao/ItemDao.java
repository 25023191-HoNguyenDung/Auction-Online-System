package com.auction.server.dao;

import com.auction.server.model.Item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemDao {
    List<ItemDao> findBySellerId(UUID sellerId);//tìm tất cả item
    void save(Item item);
    void update(Item item);
    void delete(UUID itemId);
    Optional<Item> findById(UUID itemId);
    List<Item> findAll();
    List<Item> findByCategory(String category);

}
