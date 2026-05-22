package com.auction.client.controller;

import com.auction.client.model.AuctionItem;
import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BidHistoryController {

    // ── FXML ────────────────────────────────────────────────────
    @FXML private ListView<BidEntry> activityListView;

    // ── Data ─────────────────────────────────────────────────────
    private static final double MOCK_BALANCE = 50_000.0;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");

    // ── Lifecycle ─────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupListView();
        loadMockData();
    }

    // ── ListView cell factory ─────────────────────────────────────
    private void setupListView() {
        activityListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BidEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                // Status badge
                Label badge = new Label(entry.status);
                badge.setStyle(badgeStyle(entry.status));
                badge.setMinWidth(90);

                // Item name
                Label item = new Label(entry.itemName);
                item.setStyle("-fx-text-fill: #f5f0e6; -fx-font-size: 13px;" +
                              " -fx-font-weight: bold; -fx-font-family: 'Arial';");
                item.setMaxWidth(300);

                // Amount
                Label amount = new Label(String.format("$%,.0f", entry.amount));
                amount.setStyle("-fx-text-fill: #f0b429; -fx-font-size: 13px;" +
                                " -fx-font-weight: bold; -fx-font-family: 'Arial';");

                // Timestamp
                Label time = new Label(entry.time.format(FMT));
                time.setStyle("-fx-text-fill: #5a5444; -fx-font-size: 11px;" +
                              " -fx-font-family: 'Arial';");

                Region spacer1 = new Region();
                HBox.setHgrow(spacer1, javafx.scene.layout.Priority.ALWAYS);

                Region spacer2 = new Region();
                spacer2.setMinWidth(24);

                HBox row = new HBox(12, badge, item, spacer1, amount, spacer2, time);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 14 18;");

                setGraphic(row);
                setStyle("-fx-background-color: transparent;");
            }
        });
    }

    // ── Mock data ────────────────────────────────────────────────
    private void loadMockData() {
        List<BidEntry> entries = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        String bidder = UserSession.getInstance().isLoggedIn()
                ? UserSession.getInstance().getCurrentUser().getUsername()
                : "@collector_a";

        entries.add(new BidEntry("Pioneer Zenith Hybrid",  245_000, "WINNING",  now.minusMinutes(3)));
        entries.add(new BidEntry("Ethereal Horizon",         18_900, "WINNING",  now.minusMinutes(28)));
        entries.add(new BidEntry("Vanguard Tourbillon",      82_400, "OUTBID",   now.minusHours(1)));
        entries.add(new BidEntry("Neon Phantom",              9_500, "OUTBID",   now.minusHours(2)));
        entries.add(new BidEntry("Wraith Stealth Tender",   512_000, "WINNING",  now.minusHours(3)));
        entries.add(new BidEntry("Quantum X Laptop",          4_200, "OUTBID",   now.minusHours(5)));
        entries.add(new BidEntry("Sapphire Ring 3ct",        10_000, "PENDING",  now.minusHours(8)));
        entries.add(new BidEntry("Rolex Daytona 2024",       25_000, "PENDING",  now.minusDays(1)));

        activityListView.getItems().setAll(entries);
    }

    // ── Badge style helper ────────────────────────────────────────
    private String badgeStyle(String status) {
        String bg, fg;
        switch (status) {
            case "WINNING" -> { bg = "rgba(74,222,128,0.15)";  fg = "#4ade80"; }
            case "OUTBID"  -> { bg = "rgba(239,68,68,0.15)";   fg = "#ef4444"; }
            default        -> { bg = "rgba(245,158,11,0.15)";  fg = "#f59e0b"; }
        }
        return "-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
               " -fx-font-size:10px; -fx-font-weight:bold; -fx-font-family:'Arial';" +
               " -fx-background-radius:20; -fx-padding:3 10; -fx-alignment:center;";
    }

    // ── Navigation handlers ───────────────────────────────────────
    @FXML
    private void handleNavAuctions() {
        NavigationUtils.navigateTo(
                "/com/auction/client/view/AuctionList.fxml", "Live Auctions");
    }

    @FXML
    private void handleNavHistory() {
        // Already here — no-op
    }

    @FXML
    private void handleLogout() {
        NavigationUtils.logout();
    }

    // ── Inner model ───────────────────────────────────────────────
    public static class BidEntry {
        public final String        itemName;
        public final double        amount;
        public final String        status;   // WINNING | OUTBID | PENDING
        public final LocalDateTime time;

        public BidEntry(String itemName, double amount,
                        String status, LocalDateTime time) {
            this.itemName = itemName;
            this.amount   = amount;
            this.status   = status;
            this.time     = time;
        }
    }
}