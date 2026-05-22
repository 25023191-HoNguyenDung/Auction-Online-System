package com.auction.client.viewmodel;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.auction.client.model.AuctionItem;
import com.auction.client.network.ClientMessageSender;
import com.auction.client.network.ServerConnection;
import com.auction.client.network.ServerEventListener;
import com.auction.common.protocol.*;

/**
 * AuctionListViewModel
 * - Toàn bộ logic filter/sort/search
 * - Mock data khớp với cấu trúc server (Auction + Item)
 * - Khi server xong: thay loadData() bằng gọi network
 */
public class AuctionListViewModel {
    private final List<AuctionItem> allItems      = new ArrayList<>();
    private final List<AuctionItem> filteredItems = new ArrayList<>();
    private final ClientMessageSender sender      = new ClientMessageSender();
    private final ProtocolMapper mapper           = new ProtocolMapper();

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
        if (!ServerConnection.getInstance().isConnected()) return;

        try {
            CompletableFuture<MessageEnvelope> future = new CompletableFuture<>();
            String messageId = sender.sendListAuctions(0, 0, 100, "");
            ServerEventListener.getInstance().onResponse(messageId, future::complete);

            MessageEnvelope response = future.get(5, TimeUnit.SECONDS);

            if (response.getType() == MessageType.LIST_AUCTIONS_RES) {
                ListAuctionsResPayload payload = mapper.parsePayload(response, ListAuctionsResPayload.class);
                for (AuctionSummaryItem s : payload.getAuctions()) {
                    LocalDateTime endTime = LocalDateTime.ofInstant(s.getEndTime(), ZoneOffset.UTC);
                    allItems.add(new AuctionItem(
                            s.getAuctionId(),
                            0L, 0L, "",
                            s.getItemName(), "",
                            "", s.getStatus(),
                            0, s.getCurrentHighestBid().doubleValue(),
                            LocalDateTime.now(), endTime,
                            null, 0
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi load auctions: " + e.getMessage());
        }

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