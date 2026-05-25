package com.auction.client.viewmodel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.auction.client.network.ServerConnection;
import com.auction.client.network.ClientMessageSender;
import com.auction.client.network.ServerEventListener;
import com.auction.client.sessions.UserSession;
import com.auction.common.protocol.MessageEnvelope;
import com.auction.common.protocol.MessageType;
import com.auction.common.protocol.ListAuctionsResPayload;
import com.auction.common.protocol.AuctionSummaryItem;
import com.auction.common.protocol.ProtocolMapper;
import com.auction.common.protocol.ErrorPayload;
import com.auction.client.model.AuctionItem;

/**
 * AuctionListViewModel
 * - All filtering/sorting/search logic
 * - Mock data matches server structure (Auction + Item)
 * - When server ready: replace loadData() with network call
 */
public class AuctionListViewModel {
    private static final List<AuctionItem> allItems      = new ArrayList<>();
    private final List<AuctionItem> filteredItems        = new ArrayList<>();

    private String filterStatus        = "ALL";
    private Set<String> filterCategories = new HashSet<>(); // empty = ALL
    private String keyword             = "";
    private double priceMin            = 0;
    private double priceMax            = Double.MAX_VALUE;
    private String sortBy              = "NEWEST";

    // -- Load data ---------------------------------------------
    /**
     * Phase 1 - Mock data
     * Phase 2 - Network data
     */
    public void loadData() {
        allItems.clear();

        // 1. Check network connection status
        ServerConnection connection = ServerConnection.getInstance();
        if (!connection.isConnected()) {
            System.err.println("No server connection. Cannot load real auctions!");
            applyFilters();
            return;
        }
        try {
            CompletableFuture<MessageEnvelope> responseFuture = new CompletableFuture<>();
            ClientMessageSender sender = new ClientMessageSender();
            
            // Get current user's ID
            long userId = UserSession.getInstance().isLoggedIn() 
                ? UserSession.getInstance().getCurrentUser().getId() 
                : 0L;
            // Send request to retrieve all auctions
            String messageId = sender.sendListAuctions(userId, 1, 100, null);
            // Register callback to await server response
            ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);
            // Wait up to 5 seconds for server response
            MessageEnvelope resEnvelope = responseFuture.get(5, TimeUnit.SECONDS);
            // Check if server returned an error package
            if (resEnvelope.getType() == MessageType.ERROR_RES) {
                ErrorPayload err = new ProtocolMapper().parsePayload(resEnvelope, ErrorPayload.class);
                System.err.println("Failed to load auctions from server: " + err.getMessage());
                applyFilters();
                return;
            }
            // Parse list from server
            ListAuctionsResPayload res = new ProtocolMapper().parsePayload(resEnvelope, ListAuctionsResPayload.class);
            
            // Populate real data
            for (AuctionSummaryItem summary : res.getAuctions()) {
                if ("OPEN".equalsIgnoreCase(summary.getStatus())) {
                    continue;
                }
                allItems.add(new AuctionItem(
                    summary.getAuctionId(), 
                    0L, // itemId (not needed for list view)
                    summary.getSellerId(), // sellerId from DB
                    summary.getSellerName() != null ? summary.getSellerName() : "Authorized Seller", // Seller name from DB
                    summary.getItemName(), // Real name from DB
                    summary.getDescription(), // Real description from DB
                    summary.getCategory(), // Real category from DB
                    summary.getStatus(), 
                    summary.getStartingPrice() != null ? summary.getStartingPrice().doubleValue() : summary.getCurrentHighestBid().doubleValue(), // Starting Price
                    summary.getCurrentHighestBid().doubleValue(), // Current Price
                    LocalDateTime.now().minusMinutes(5), // Temporary start time
                    LocalDateTime.ofInstant(summary.getEndTime(), ZoneId.systemDefault()), // Precise end time from DB!
                    null, // imageUrl
                    summary.getBidHistory() != null ? summary.getBidHistory().size() : 0, // totalBids
                    summary.getBidHistory() // Real bid history from DB
                ));
            }
        } catch (Exception e) {
            System.err.println("Error while fetching auctions from server: " + e.getMessage());
            e.printStackTrace();
        }
        // Apply filtering and sorting
        applyFilters();
    }

    // -- Setters (called by Controller) ------------------------
    public void setKeyword(String kw) {
        this.keyword = kw == null ? "" : kw.trim().toLowerCase();
    }

    public void setFilterStatus(String status) {
        this.filterStatus = status == null ? "ALL" : status;
    }

    public void setFilterCategories(Set<String> categories) {
        this.filterCategories = categories == null ? new HashSet<>() : categories;
    }

    public void setPriceRange(double min, double max) {
        this.priceMin = min;
        this.priceMax = max <= 0 ? Double.MAX_VALUE : max;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy == null ? "NEWEST" : sortBy;
    }

    // -- Apply filters + sort ----------------------------------
    public List<AuctionItem> applyFilters() {
        filteredItems.clear();
        filteredItems.addAll(
            allItems.stream()
                .filter(this::matchStatus)
                .filter(this::matchCategory)
                .filter(this::matchPrice)
                .filter(this::matchKeyword)
                .collect(Collectors.toList())
        );
        applySort();
        return new ArrayList<>(filteredItems);
    }

    private boolean matchStatus(AuctionItem item) {
        if ("ALL".equals(filterStatus)) return true;
        // Map UI status -> server status
        return switch (filterStatus) {
            case "LIVE"         -> item.isRunning();
            case "ENDING_SOON"  -> item.isEndingSoon();
            case "PENDING"      -> item.isPending();
            case "CLOSED"       -> item.isClosed();
            default             -> true;
        };
    }

    private boolean matchCategory(AuctionItem item) {
        if (filterCategories.isEmpty()) return true;
        return filterCategories.stream()
            .anyMatch(cat -> cat.equalsIgnoreCase(item.getCategory()));
    }

    private boolean matchPrice(AuctionItem item) {
        return item.getCurrentPrice() >= priceMin
            && item.getCurrentPrice() <= priceMax;
    }

    private boolean matchKeyword(AuctionItem item) {
        if (keyword.isEmpty()) return true;
        return item.getItemName().toLowerCase().contains(keyword)
            || item.getDescription().toLowerCase().contains(keyword)
            || item.getCategory().toLowerCase().contains(keyword)
            || item.getSellerName().toLowerCase().contains(keyword);
    }

    private void applySort() {
        switch (sortBy) {
            case "PRICE_ASC"   -> filteredItems.sort(
                Comparator.comparingDouble(AuctionItem::getCurrentPrice));
            case "PRICE_DESC"  -> filteredItems.sort(
                (a, b) -> Double.compare(b.getCurrentPrice(), a.getCurrentPrice()));
            case "ENDING_SOON" -> filteredItems.sort(
                Comparator.comparingInt(AuctionItem::secondsLeft));
            default -> {}
        }
    }

    // -- Getters -----------------------------------------------
    public List<AuctionItem> getFilteredItems() { return new ArrayList<>(filteredItems); }
    public List<AuctionItem> getAllItems()       { return new ArrayList<>(allItems); }
    public int getTotalCount()                  { return allItems.size(); }
    public int getFilteredCount()               { return filteredItems.size(); }

    // Allows resetting the mock data on logout
    public static void clearData() {
        allItems.clear();
    }
}
