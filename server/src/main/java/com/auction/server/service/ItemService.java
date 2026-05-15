package com.auction.server.service;

import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.ItemDao;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.pattern.ItemFactory;
import com.auction.server.model.Auction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ItemService {

    private final ItemDao    itemDao;
    private final AuctionDao auctionDao;

    public ItemService(ItemDao itemDao, AuctionDao auctionDao) {
        this.itemDao    = itemDao;
        this.auctionDao = auctionDao;
    }

    public Item createItem(long sellerId,
                           String itemName,
                           String description,
                           String category,
                           BigDecimal startingPrice,
                           String imageUrl,
                           double reservePrice) {

        validateItemFields(itemName, startingPrice, reservePrice);

        Item item = new Item();
        item.setSellerId(sellerId);
        item.setItemName(itemName.trim());
        item.setDescription(description);
        item.setCategory(category.toUpperCase());
        item.setStartingPrice(startingPrice);
        item.setCurrentPrice(startingPrice);   // ban đầu current = starting
        item.setImageUrl(imageUrl);
        item.setReserve_price(reservePrice);

        return itemDao.save(item);
    }

    
    public Item createTypedItem(long sellerId,
                                String itemName,
                                String description,
                                String category,
                                BigDecimal startingPrice,
                                String imageUrl,
                                double reservePrice,
                                Object... additionalParams) {

        validateItemFields(itemName, startingPrice, reservePrice);

        // ItemFactory tự xử lý logic tạo item dựa trên category và additionalParams.
        Item item = ItemFactory.createItem(
                category, 0L, sellerId,
                itemName, description,
                startingPrice, startingPrice,
                imageUrl, reservePrice,
                additionalParams
        );

        return itemDao.save(item);
    }


    public Item getById(long itemId) {
        return itemDao.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item không tồn tại, id=" + itemId));
    }

    public Optional<Item> findById(long itemId) {
        return itemDao.findById(itemId);
    }

    public List<Item> findBySeller(long sellerId) {
        return itemDao.findBySellerId(sellerId);
    }

    public List<Item> getAllItems() {
        return itemDao.findAll();
    }


    public Item updateItem(long itemId,
                           String newName,
                           String newDescription,
                           double newReservePrice,
                           String newImageUrl) {

        Item item = getById(itemId);
        guardActiveAuction(itemId, "cập nhật");

        if (newName        != null && !newName.isBlank())        item.setItemName(newName.trim()) ;
        if (newDescription != null && !newDescription.isBlank()) item.setDescription(newDescription) ;
        if (newReservePrice > 0)                                 item.setReserve_price(newReservePrice) ;
        if (newImageUrl    != null && !newImageUrl.isBlank())    item.setImageUrl(newImageUrl) ;

        return itemDao.update(item);
    }


    public boolean deleteItem(long itemId) {
        guardActiveAuction(itemId, "delete");
        boolean deleted = itemDao.deleteById(itemId);
        if (!deleted)
            System.out.println("ItemService: Failed to delete item, id = " + itemId);
        return deleted;
    }

    // Kiểm tra xem item có đang tham gia phiên đấu giá nào ở trạng thái OPEN/RUNNING không. do chỉ là helper nội bộ nên dùng private.
    private void guardActiveAuction(long itemId, String action) {

        for (Auction a : auctionDao.findAll()) {

            if (a.getItem_id() == itemId &&
                (a.getStatus() == AuctionStatus.OPEN ||
                a.getStatus() == AuctionStatus.RUNNING)) {

                throw new IllegalStateException(
                        "Cannot " + action + " item id = " + itemId +
                        ". This item is currently in an active auction!"
                );
            }
        }
    }

    // Validate các trường cơ bản của item khi tạo mới hoặc cập nhật.
    private void validateItemFields(String itemName, BigDecimal startingPrice, double reservePrice) {
        if (itemName == null || itemName.isBlank())
            throw new IllegalArgumentException("Item name is required and cannot be blank.");
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Starting price must be a positive number.");
        if (reservePrice < 0)
            throw new IllegalArgumentException("Reserve price cannot be negative.");
    }
}