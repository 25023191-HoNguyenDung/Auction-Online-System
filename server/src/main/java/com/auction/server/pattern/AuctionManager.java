package com.auction.server.pattern;
import com.auction.server.model.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    
    private static AuctionManager instance;

    private List<Product> products;
    private List<Auction> auctions;

    private AuctionManager() {
        products = new ArrayList<>();
        auctions = new ArrayList<>();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }
    
    // Methods to add products
    public void addProduct(Product product) {
        for (Product p : products) {
            if (p.get_product_name().equals(product.get_product_name())) {
                throw new IllegalArgumentException("Product with the same name already exists: " + product.get_product_name());
            }
        }
        products.add(product);
    }

    // Method to remove products
    public boolean removeProduct(long product_id) {
        for (Product p : products) {
            if (p.get_product_id()==(product_id)) {
                products.remove(p);
                System.out.println("Product removed successfully: " + p.get_product_name());
                return true;
            }
        }
        System.out.println("Product not found: " + product_id);
        return false;
    }

    //Change product details
    public boolean updateProduct(String new_product_name,
                                long product_id,
                                String new_description,
                                double new_reserve_price,
                                String new_image_url) {
        try {
            for (Product p : products) {
                if (p.get_product_id()==(product_id)) {
                    for (Auction a : auctions) {
                        if (a.getItem_id()==(product_id)
                                && a.getStatus() == AuctionStatus.OPEN) {
                            throw new IllegalStateException("Cannot update product details while it "
                                    + "is in an active auction: " + p.get_product_name());
                        }
                    }

                    if (new_product_name != null && !new_product_name.isBlank())
                        p.set_product_name(new_product_name);
                    if (new_description != null && !new_description.isBlank())
                        p.set_description(new_description);
                    if (new_reserve_price > 0)
                        p.set_reserve_price(new_reserve_price);
                    if (new_image_url != null && !new_image_url.isBlank())
                        p.set_image_url(new_image_url);

                    System.out.println("Product updated successfully: " + p.get_product_name());
                    return true;
                }
            }                                                    
            System.out.println("Product not found: " + product_id); 
            return false;                                            

        } catch (IllegalStateException e) {
            System.out.println("Error updating product: " + e.getMessage());
            return false;
        }
    }

    // Method to find product by ID
    public Product findProductById(long product_id) {
        for (Product p : products) {
            if (p.get_product_id()==(product_id)) {
                return p;
            }
        }
        return null;
    }

    // Method to list all products
    public void listAllProducts() {
        if (products.isEmpty()) {
            System.out.println("No products available.");
            return;
        }
        System.out.println("\n===== ALL PRODUCTS (" + products.size() + ") =====");
        for (Product p : products) {
            System.out.println("ID: " + p.get_product_id());
            System.out.println("Name: " + p.get_product_name());
            System.out.println("Description: " + p.get_description());
            System.out.println("Reserve Price: " + p.get_reserve_price());
            System.out.println("Image URL: " + p.get_image_url());
            System.out.println("─────────────────────────────");
        }
    }   

    public List<Product> getProducts() { return products; }

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
            System.out.println("Auction added for product: " + auction.getItem_id());
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
                    + " | Product: " + a.getItem_id()
                    + " | Status: " + a.getStatus()
                    + " | Current Price: " + a.getCurrent_price());
            System.out.println("─────────────────────────────");
        }
    }

    public List<Auction> getAuctions() { return auctions; }
}    

