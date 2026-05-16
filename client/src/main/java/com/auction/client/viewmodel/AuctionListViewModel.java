package com.auction.client.viewmodel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.auction.client.model.AuctionItem;

/**
 * AuctionListViewModel
 * - Toàn bộ logic filter/sort/search
 * - Mock data khớp với cấu trúc server (Auction + Item)
 * - Khi server xong: thay loadData() bằng gọi network
 */
public class AuctionListViewModel {

    private final List<AuctionItem> allItems      = new ArrayList<>();
    private final List<AuctionItem> filteredItems = new ArrayList<>();

    private String filterStatus        = "ALL";
    private Set<String> filterCategories = new HashSet<>(); // empty = ALL
    private String keyword             = "";
    private double priceMin            = 0;
    private double priceMax            = Double.MAX_VALUE;
    private String sortBy              = "NEWEST";

    // ── Load data ─────────────────────────────────────────────
    /**
     * Giai đoạn 1 — Mock, khớp đúng cấu trúc server:
     *   status: RUNNING / PENDING / CLOSED (theo AuctionStatus server)
     *
     * Giai đoạn 2 — thay bằng:
     *   List<AuctionItem> items = serverConnection.getAuctions();
     *   allItems.addAll(items);
     */
    public void loadData() {
        allItems.clear();
        LocalDateTime now = LocalDateTime.now();

        // auctionId, itemId, sellerId, sellerName,
        // itemName, description, category, status,
        // startingPrice, currentPrice,
        // startTime, endTime, imageUrl, totalBids
        allItems.add(new AuctionItem(
            1L, 101L, 2L, "Sterling House",
            "Pioneer Zenith Hybrid", "LIMITED PRODUCTION 1 OF 50",
            "Vehicles", "RUNNING",
            180000, 245000,
            now.minusHours(2), now.plusSeconds(7685),
            null, 47));

        allItems.add(new AuctionItem(
            2L, 102L, 4L, "Marcus Gold",
            "Vanguard Tourbillon", "ROSE GOLD SKELETON EDITION",
            "Watches", "RUNNING",
            70000, 82400,
            now.minusHours(5), now.plusSeconds(704),
            null, 31));

        allItems.add(new AuctionItem(
            3L, 103L, 2L, "Sterling House",
            "Ethereal Horizon", "MIXED MEDIA ON CANVAS (2024)",
            "Art", "RUNNING",
            12000, 18900,
            now.minusHours(1), now.plusSeconds(31332),
            null, 12));

        allItems.add(new AuctionItem(
            4L, 104L, 4L, "Marcus Gold",
            "Wraith Stealth Tender", "CUSTOM CARBON SERIES",
            "Vehicles", "RUNNING",
            400000, 512000,
            now.minusHours(3), now.plusDays(1).plusHours(4),
            null, 28));

        allItems.add(new AuctionItem(
            5L, 105L, 2L, "Sterling House",
            "Neon Phantom", "DIGITAL ART 1/1 EDITION",
            "Art", "RUNNING",
            6000, 9500,
            now.minusHours(1), now.plusSeconds(5400),
            null, 15));

        allItems.add(new AuctionItem(
            6L, 106L, 4L, "Marcus Gold",
            "Quantum X Laptop", "TITANIUM EDITION 2024",
            "Electronics", "RUNNING",
            3000, 4200,
            now.minusHours(4), now.plusSeconds(500),
            null, 8));

        allItems.add(new AuctionItem(
            7L, 107L, 2L, "Sterling House",
            "Sapphire Ring 3ct", "VVS1 CERTIFIED",
            "Jewellery", "PENDING",
            10000, 10000,
            now.plusDays(1), now.plusDays(8),
            null, 0));

        applyFilters();
    }

    // ── Setters (Controller gọi) ──────────────────────────────
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

    // ── Apply filters + sort ──────────────────────────────────
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
        // Map UI status → server status
        return switch (filterStatus) {
            case "LIVE"         -> item.isRunning() && !item.isEndingSoon();
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

    // ── Getters ───────────────────────────────────────────────
    public List<AuctionItem> getFilteredItems() { return new ArrayList<>(filteredItems); }
    public List<AuctionItem> getAllItems()       { return new ArrayList<>(allItems); }
    public int getTotalCount()                  { return allItems.size(); }
    public int getFilteredCount()               { return filteredItems.size(); }
}