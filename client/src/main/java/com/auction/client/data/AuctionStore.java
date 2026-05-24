package com.auction.client.data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.auction.client.model.AuctionItem;
import com.auction.client.model.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class AuctionStore {
    private static final AuctionStore INSTANCE = new AuctionStore();

    private final ObservableList<AuctionItem> allAuctions = FXCollections.observableArrayList();
    private final ObservableList<AuctionItem> pendingAuctions = FXCollections.observableArrayList();
    private final AtomicLong nextAuctionId = new AtomicLong(10_000L);
    private final AtomicLong nextItemId = new AtomicLong(20_000L);

    private AuctionStore() {
        seed();
    }

    public static AuctionStore getInstance() {
        return INSTANCE;
    }

    public ObservableList<AuctionItem> getAllAuctions() {
        return allAuctions;
    }

    public ObservableList<AuctionItem> getPendingAuctions() {
        return pendingAuctions;
    }

    public List<AuctionItem> getVisibleAuctionList() {
        return allAuctions.stream()
            .filter(item -> !item.isPending())
            .sorted(Comparator.comparingLong(AuctionItem::getAuctionId).reversed())
            .toList();
    }

    public List<AuctionItem> getAuctionsForSeller(long sellerId) {
        List<AuctionItem> result = new ArrayList<>();
        allAuctions.stream()
            .filter(item -> item.getSellerId() == sellerId)
            .forEach(result::add);
        pendingAuctions.stream()
            .filter(item -> item.getSellerId() == sellerId)
            .forEach(result::add);
        return result;
    }

    public List<AuctionItem> getAuctionsForSeller(User seller) {
        if (seller == null) {
            return List.of();
        }

        String username = seller.getUsername();
        List<AuctionItem> result = new ArrayList<>();
        allAuctions.stream()
            .filter(item -> belongsToSeller(item, seller.getId(), username))
            .forEach(result::add);
        pendingAuctions.stream()
            .filter(item -> belongsToSeller(item, seller.getId(), username))
            .forEach(result::add);
        return result;
    }

    private boolean belongsToSeller(AuctionItem item, long sellerId, String username) {
        return item.getSellerId() == sellerId
            || (username != null && username.equalsIgnoreCase(item.getSellerName()));
    }

    public AuctionItem submitListing(User seller, String name, String description, String category,
                                     double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        String sellerName = seller != null ? seller.getUsername() : "Seller";
        long sellerId = seller != null ? seller.getId() : 0L;
        AuctionItem item = new AuctionItem(
            nextAuctionId.getAndIncrement(),
            nextItemId.getAndIncrement(),
            sellerId,
            sellerName,
            name,
            description,
            category,
            "PENDING",
            startingPrice,
            startingPrice,
            startTime,
            endTime,
            null,
            0
        );
        pendingAuctions.add(0, item);
        return item;
    }

    public void approve(AuctionItem item) {
        if (item == null) return;
        pendingAuctions.remove(item);
        item.setStatus("RUNNING");
        if (!allAuctions.contains(item)) {
            allAuctions.add(0, item);
        }
    }

    public void reject(AuctionItem item) {
        pendingAuctions.remove(item);
    }

    public void end(AuctionItem item) {
        if (item != null) {
            item.setStatus("CLOSED");
        }
    }

    public void remove(AuctionItem item) {
        if (item == null) return;
        pendingAuctions.remove(item);
        allAuctions.remove(item);
    }

    private void seed() {
        // Mock data removed. Connected to database on server.
    }
}
