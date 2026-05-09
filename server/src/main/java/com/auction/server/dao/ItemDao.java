package com.auction.server.dao;

import com.auction.server.model.Item;
import java.util.List;
import java.util.Optional;

public interface ItemDao {
    Optional<Item> findById(long id);
    List<Item> findBySellerId(long sellerId);
    List<Item> findAll();
    Item save(Item item);
    Item update(Item item);
    boolean deleteById(long id);
}
