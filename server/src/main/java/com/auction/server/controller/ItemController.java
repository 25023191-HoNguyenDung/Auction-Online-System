package com.auction.server.controller;

import com.auction.server.model.Item;
import com.auction.server.service.ItemService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    public Map<String, Object> handleListItems() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Item> items = itemService.getAllItems();
            response.put("type", "LIST_ITEMS_RES");
            response.put("success", true);
            response.put("items", items.stream().map(this::itemToMap).toList());
        } catch (Exception e) {
            return errorResponse("Error when fetching items: " + e.getMessage());
        }
        return response;
    }

    public Map<String, Object> handleGetItem(Map<String, Object> payload) {
        try {
            long itemId = toLong(payload.get("itemId"));
            Item item = itemService.getById(itemId);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "GET_ITEM_RES");
            response.put("success", true);
            response.put("item", itemToMap(item));
            return response;

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Error when fetching item: " + e.getMessage());
        }
    }


    public Map<String, Object> handleCreateItem(Map<String, Object> payload) {
        try {
            long   sellerId      = toLong(payload.get("sellerId"));
            String itemName      = (String) payload.get("itemName");
            String description   = (String) payload.get("description");
            String category      = (String) payload.get("category");
            BigDecimal startingPrice = new BigDecimal(payload.get("startingPrice").toString());
            String imageUrl      = (String) payload.getOrDefault("imageUrl", "");
            double reservePrice  = Double.parseDouble(payload.getOrDefault("reservePrice", "0").toString());

            Item created = itemService.createItem(
                    sellerId, itemName, description, category,
                    startingPrice, imageUrl, reservePrice);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "CREATE_ITEM_RES");
            response.put("success", true);
            response.put("item", itemToMap(created));
            return response;

        } catch (IllegalArgumentException e) {
            // Lỗi validate từ ItemService (tên trống, giá âm, v.v.)
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Error when creating item: " + e.getMessage());
        }
    }


    public Map<String, Object> handleUpdateItem(Map<String, Object> payload) {
        try {
            long   itemId        = toLong(payload.get("itemId"));
            String newName       = (String) payload.getOrDefault("itemName", null);
            String newDesc       = (String) payload.getOrDefault("description", null);
            double newReserve    = Double.parseDouble(payload.getOrDefault("reservePrice", "0").toString());
            String newImageUrl   = (String) payload.getOrDefault("imageUrl", null);

            Item updated = itemService.updateItem(itemId, newName, newDesc, newReserve, newImageUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "UPDATE_ITEM_RES");
            response.put("success", true);
            response.put("item", itemToMap(updated));
            return response;

        } catch (IllegalStateException e) {
            // Item đang trong phiên đấu giá → không được sửa
            return errorResponse(e.getMessage());
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Error when updating item: " + e.getMessage());
        }
    }


    public Map<String, Object> handleDeleteItem(Map<String, Object> payload) {
        try {
            long itemId = toLong(payload.get("itemId"));
            boolean deleted = itemService.deleteItem(itemId);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "DELETE_ITEM_RES");
            response.put("success", deleted);
            response.put("message", deleted ? "Deleted item successfully" : "Item not found");
            return response;

        } catch (IllegalStateException e) {
            return errorResponse(e.getMessage());
        } catch (Exception e) {
            return errorResponse("Error when deleting item: " + e.getMessage());
        }
    }

    // --- Helper methods ---

    /** Chuyển Item object thành Map để encode JSON gửi về client */
    private Map<String, Object> itemToMap(Item item) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemId",        item.getItemId());
        map.put("sellerId",      item.getSellerId());
        map.put("itemName",      item.getItemName());
        map.put("description",   item.getDescription());
        map.put("category",      item.getCategory());
        map.put("startingPrice", item.getStartingPrice());
        map.put("currentPrice",  item.getCurrentPrice());
        map.put("imageUrl",      item.getImageUrl());
        map.put("reservePrice",  item.getReserve_price());
        return map;
    }

    private long toLong(Object value) {
        if (value == null) throw new IllegalArgumentException("Thiếu tham số ID.");
        return Long.parseLong(value.toString());
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("type", "ERROR_RES");
        err.put("success", false);
        err.put("message", message);
        return err;
    }
}