package com.auction.client.viewmodel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.auction.client.model.AuctionItem;

/**
 * AuctionListViewModel
 * - Giữ toàn bộ logic lọc, sort, search
 * - Controller chỉ gọi method của ViewModel, không xử lý logic trực tiếp
 * - Khi có server: chỉ thay loadMockData() bằng API call, Controller không đổi
 */
public class AuctionListViewModel {

    // ── State ─────────────────────────────────────────────────
    private final List<AuctionItem> allItems      = new ArrayList<>();
    private final List<AuctionItem> filteredItems = new ArrayList<>();

    private String filterStatus   = "ALL";   // ALL | LIVE | ENDING_SOON
    private String filterCategory = "ALL";   // ALL | Art | Electronics | Vehicles | Watches
    private String keyword        = "";
    private double priceMin       = 0;
    private double priceMax       = Double.MAX_VALUE;
    private String sortBy         = "NEWEST"; // NEWEST | PRICE_ASC | PRICE_DESC | ENDING_SOON

    // ── Load data ─────────────────────────────────────────────
    /**
     * Giai đoạn 1: Mock data
     * Giai đoạn 2: thay bằng → allItems.addAll(serverConnection.getAuctions());
     */
    public void loadData() {
        allItems.clear();
        long now = System.currentTimeMillis() / 1000;

        allItems.add(new AuctionItem(1, "Pioneer Zenith Hybrid",  "LIMITED PRODUCTION 1 OF 50",
            "Vehicles",    "LIVE",        180000, 245000, 47, now + 7685,   null, 2, "Sterling House"));
        allItems.add(new AuctionItem(2, "Vanguard Tourbillon",    "ROSE GOLD SKELETON EDITION",
            "Watches",     "ENDING_SOON",  70000,  82400, 31, now + 704,    null, 4, "Marcus Gold"));
        allItems.add(new AuctionItem(3, "Ethereal Horizon",       "MIXED MEDIA ON CANVAS (2024)",
            "Art",         "LIVE",         12000,  18900, 12, now + 31332,  null, 2, "Sterling House"));
        allItems.add(new AuctionItem(4, "Wraith Stealth Tender",  "CUSTOM CARBON SERIES",
            "Vehicles",    "LIVE",        400000, 512000, 28, now + 100800, null, 4, "Marcus Gold"));
        allItems.add(new AuctionItem(5, "Neon Phantom",           "DIGITAL ART 1/1 EDITION",
            "Art",         "LIVE",          6000,   9500, 15, now + 5400,   null, 2, "Sterling House"));
        allItems.add(new AuctionItem(6, "Quantum X Laptop",       "TITANIUM EDITION 2024",
            "Electronics", "ENDING_SOON",   3000,   4200,  8, now + 500,    null, 4, "Marcus Gold"));

        applyFilters();
    }

    // ── Filter setters (Controller gọi khi user thao tác) ────
    public void setKeyword(String keyword) {
        this.keyword = keyword == null ? "" : keyword.trim().toLowerCase();
    }

    public void setFilterStatus(String status) {
        this.filterStatus = status == null ? "ALL" : status;
    }

    public void setFilterCategory(String category) {
        this.filterCategory = category == null ? "ALL" : category;
    }

    public void setPriceRange(double min, double max) {
        this.priceMin = min;
        this.priceMax = max <= 0 ? Double.MAX_VALUE : max;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy == null ? "NEWEST" : sortBy;
    }

    // ── Apply tất cả filter + sort ────────────────────────────
    public List<AuctionItem> applyFilters() {
        filteredItems.clear();
        filteredItems.addAll(
            allItems.stream()
                .filter(this::matchesStatus)
                .filter(this::matchesCategory)
                .filter(this::matchesPrice)
                .filter(this::matchesKeyword)
                .collect(Collectors.toList())
        );
        applySort();
        return new ArrayList<>(filteredItems);
    }

    private boolean matchesStatus(AuctionItem item) {
        return filterStatus.equals("ALL") || item.getStatus().equals(filterStatus);
    }

    private boolean matchesCategory(AuctionItem item) {
        return filterCategory.equals("ALL") || item.getCategory().equals(filterCategory);
    }

    private boolean matchesPrice(AuctionItem item) {
        return item.getCurrentBid() >= priceMin && item.getCurrentBid() <= priceMax;
    }

    private boolean matchesKeyword(AuctionItem item) {
        if (keyword.isEmpty()) return true;
        return item.getTitle().toLowerCase().contains(keyword)
            || item.getSubtitle().toLowerCase().contains(keyword)
            || item.getCategory().toLowerCase().contains(keyword)
            || item.getSellerName().toLowerCase().contains(keyword);
    }

    private void applySort() {
        switch (sortBy) {
            case "PRICE_ASC"    -> filteredItems.sort(Comparator.comparingDouble(AuctionItem::getCurrentBid));
            case "PRICE_DESC"   -> filteredItems.sort((a, b) -> Double.compare(b.getCurrentBid(), a.getCurrentBid()));
            case "ENDING_SOON"  -> filteredItems.sort(Comparator.comparingInt(AuctionItem::secondsLeft));
            default             -> {} // NEWEST: giữ thứ tự server
        }
    }

    // ── Getters ───────────────────────────────────────────────
    public List<AuctionItem> getFilteredItems() { return new ArrayList<>(filteredItems); }
    public List<AuctionItem> getAllItems()       { return new ArrayList<>(allItems); }
    public int getTotalCount()                  { return allItems.size(); }
    public int getFilteredCount()               { return filteredItems.size(); }
}
