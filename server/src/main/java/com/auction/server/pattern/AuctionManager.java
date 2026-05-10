package com.auction.server.pattern;
import com.auction.server.model.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    
    private static AuctionManager instance;

    private List<Item> items;
    private List<Auction> auctions;

    private AuctionManager() {
        items = new ArrayList<>();
        auctions = new ArrayList<>();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }
    
    // Methods to add items
    public void addItem(Item item) {
        for (Item p : items) {
            if (p.getItemName().equals(item.getItemName())) {
                throw new IllegalArgumentException("item with the same name already exists: " + item.getItemName());
            }
        }
        items.add(item);
    }

    // Method to remove items
    public boolean removeItem(long item_id) {
        for (Item p : items) {
            if (p.getItemId()==(item_id)) {
                items.remove(p);
                System.out.println("item removed successfully: " + p.getItemName());
                return true;
            }
        }
        System.out.println("item not found: " + item_id);
        return false;
    }

    //Change item details
    public boolean updateItem(String new_item_name,
                                long item_id,
                                String new_description,
                                double new_reserve_price,
                                String new_image_url) {
        try {
            for (Item p : items) {
                if (p.getItemId()==(item_id)) {
                    for (Auction a : auctions) {
                        if (a.getItem_id()==(item_id)
                                && a.getStatus() == AuctionStatus.OPEN) {
                            throw new IllegalStateException("Cannot update item details while it "
                                    + "is in an active auction: " + p.getItemName());
                        }
                    }

                    if (new_item_name != null && !new_item_name.isBlank())
                        p.setItemName(new_item_name);
                    if (new_description != null && !new_description.isBlank())
                        p.setDescription(new_description);
                    if (new_reserve_price > 0)
                        p.setReserve_price(new_reserve_price);
                    if (new_image_url != null && !new_image_url.isBlank())
                        p.setImageUrl(new_image_url);

                    System.out.println("item updated successfully: " + p.getItemName());
                    return true;
                }
            }                                                    
            System.out.println("item not found: " + item_id);
            return false;                                            

        } catch (IllegalStateException e) {
            System.out.println("Error updating item: " + e.getMessage());
            return false;
        }
    }

    // Method to find item by ID
    public Item findItemById(long item_id) {
        for (Item p : items) {
            if (p.getItemId()==(item_id)) {
                return p;
            }
        }
        return null;
    }

    // Method to list all Item
    public void listAllItems() {
        if (items.isEmpty()) {
            System.out.println("No item available.");
            return;
        }
        System.out.println("\n===== ALL ITEMS (" + items.size() + ") =====");
        for (Item p : items) {
            System.out.println("ID: " + p.getItemId());
            System.out.println("Name: " + p.getItemName());
            System.out.println("Description: " + p.getDescription());
            System.out.println("Reserve Price: " + p.getReserve_price());
            System.out.println("Image URL: " + p.getImageUrl());
            System.out.println("─────────────────────────────");
        }
    }   

    public List<Item> getItems() { return items; }

    // Add Auction
    public void addAuction(Auction auction) {
        try {
            for (Auction a : auctions) {
                if (a.getId()==(auction.getId())) {
                    throw new IllegalArgumentException("Auction ID already exists: "
                            + auction.getId());
                }
            }
            auctions.add(auction);
            System.out.println("Auction added for item: " + auction.getItem_id());
        } catch (IllegalArgumentException e) {
            System.out.println("Error adding auction: " + e.getMessage());
        }
    }

    // Remove Auction
    public boolean removeAuction(long auction_id) {
        Auction toRemove = findAuctionById(auction_id);
        if (toRemove == null) {
            System.out.println(" Auction not found: " + auction_id);
            return false;
        }
        auctions.remove(toRemove);
        System.out.println(" Auction removed: " + auction_id);
        return true;
    }

    // Find Auction by ID
    public Auction findAuctionById(long auction_id) {
        for (Auction a : auctions) {
            if (a.getId()==(auction_id)) {
                return a;
            }
        }
        return null;
    }

    // List All Auctions
    public void listAllAuctions() {
        if (auctions.isEmpty()) {
            System.out.println("No auctions available.");
            return;
        }
        System.out.println("\n===== ALL AUCTIONS (" + auctions.size() + ") =====");
        for (Auction a : auctions) {
            System.out.println("ID: " + a.getId()
                    + " | item: " + a.getItem_id()
                    + " | Status: " + a.getStatus()
                    + " | Current Price: " + a.getCurrent_price());
            System.out.println("─────────────────────────────");
        }
    }

    public List<Auction> getAuctions() { return auctions; }
}    

