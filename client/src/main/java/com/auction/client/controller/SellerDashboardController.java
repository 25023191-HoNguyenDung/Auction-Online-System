package com.auction.client.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.auction.client.model.AuctionItem;
import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class SellerDashboardController {

    // ── Sidebar ───────────────────────────────────────────────
    @FXML private Label  navInitialLabel;
    @FXML private Label  sideInitialLabel;
    @FXML private Label  sideNameLabel;
    @FXML private Label  quickActiveLabel;
    @FXML private Label  quickPendingLabel;
    @FXML private Label  quickBidsLabel;
    @FXML private Button sideOverview;
    @FXML private Button sideMyAuctions;
    @FXML private Button sideCreate;
    @FXML private Button sideBids;
    @FXML private Button sideHistory;

    // ── Stat cards ────────────────────────────────────────────
    @FXML private Label cardActive;
    @FXML private Label cardTotalBids;
    @FXML private Label cardRevenue;
    @FXML private Label cardPending;

    // ── My Auctions tab ───────────────────────────────────────
    @FXML private TextField              searchMyAuctions;
    @FXML private ComboBox<String>       filterMyStatus;
    @FXML private TableView<AuctionItem> myAuctionsTable;
    @FXML private TableColumn<AuctionItem, String> colMyId;
    @FXML private TableColumn<AuctionItem, String> colMyItem;
    @FXML private TableColumn<AuctionItem, String> colMyCategory;
    @FXML private TableColumn<AuctionItem, String> colMyBid;
    @FXML private TableColumn<AuctionItem, String> colMyBids;
    @FXML private TableColumn<AuctionItem, String> colMyStatus;
    @FXML private TableColumn<AuctionItem, String> colMyEnds;
    @FXML private TableColumn<AuctionItem, String> colMyAction;

    // ── Create Listing tab ────────────────────────────────────
    @FXML private TextField        formItemName;
    @FXML private ComboBox<String> formCategory;
    @FXML private TextField        formStartPrice;
    @FXML private TextField        formReservePrice;
    @FXML private ComboBox<String> formDuration;
    @FXML private ComboBox<String> formCondition;
    @FXML private TextArea         formDescription;
    @FXML private Label            formMessage;
    @FXML private Button           btnSubmitListing;

    // Preview
    @FXML private Label previewTitle;
    @FXML private Label previewCategory;
    @FXML private Label previewPrice;
    @FXML private Label previewReserve;
    @FXML private Label previewDuration;
    @FXML private Label previewEmoji;

    // ── Bids Received tab ─────────────────────────────────────
    @FXML private TableView<String[]>              bidsReceivedTable;
    @FXML private TableColumn<String[], String>    colBidItem;
    @FXML private TableColumn<String[], String>    colBidder;
    @FXML private TableColumn<String[], String>    colBidAmount;
    @FXML private TableColumn<String[], String>    colBidTime;
    @FXML private TableColumn<String[], String>    colBidStatus;

    // ── Mock data ─────────────────────────────────────────────
    private final ObservableList<AuctionItem> myAuctions = FXCollections.observableArrayList();
    private final ObservableList<String[]>    bidsData   = FXCollections.observableArrayList();

    // ── Lifecycle ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        loadSellerInfo();
        setupSidebarButtons();
        setupMyAuctionsTab();
        setupCreateListingTab();
        setupBidsTab();
        loadMockData();
    }

    // ── Seller info ───────────────────────────────────────────
    private void loadSellerInfo() {
        if (!UserSession.getInstance().isLoggedIn()) return;
        String name = UserSession.getInstance().getCurrentUser().getUsername();
        String initial = name != null && !name.isEmpty()
            ? String.valueOf(name.charAt(0)).toUpperCase() : "S";

        if (navInitialLabel  != null) navInitialLabel.setText(initial);
        if (sideInitialLabel != null) sideInitialLabel.setText(initial);
        if (sideNameLabel    != null) sideNameLabel.setText(name);
    }

    // ── Sidebar navigation ────────────────────────────────────
    private void setupSidebarButtons() {
        // Simple tab switching via TabPane index would need fx:id on TabPane
        // For now sidebar buttons are visual-only placeholders
    }

    // ── My Auctions tab ───────────────────────────────────────
    private void setupMyAuctionsTab() {
        filterMyStatus.getItems().addAll("All", "Live", "Ending Soon", "Pending", "Closed");
        filterMyStatus.getSelectionModel().selectFirst();
        filterMyStatus.setOnAction(e -> applyAuctionFilter());

        searchMyAuctions.textProperty().addListener((obs, old, val) -> applyAuctionFilter());

        colMyId      .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAuctionId())));
        colMyItem    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        colMyCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        colMyBid     .setCellValueFactory(d -> new SimpleStringProperty(String.format("$%,.0f", d.getValue().getCurrentPrice())));
        colMyBids    .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getTotalBids())));
        colMyStatus  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayStatus()));
        colMyEnds    .setCellValueFactory(d -> new SimpleStringProperty(formatTime(d.getValue().secondsLeft())));
        colMyAction  .setCellValueFactory(d -> new SimpleStringProperty("Edit / End"));

        myAuctionsTable.setItems(myAuctions);
    }

    private void applyAuctionFilter() {
        String keyword = searchMyAuctions.getText().trim().toLowerCase();
        String status  = filterMyStatus.getValue();

        ObservableList<AuctionItem> filtered = FXCollections.observableArrayList();
        for (AuctionItem item : myAuctions) {
            boolean matchKw = keyword.isEmpty()
                || item.getItemName().toLowerCase().contains(keyword);
            boolean matchSt = "All".equals(status)
                || item.getDisplayStatus().equalsIgnoreCase(status)
                || (status.equals("Ending Soon") && item.isEndingSoon())
                || (status.equals("Pending")     && item.isPending())
                || (status.equals("Closed")      && item.isClosed());
            if (matchKw && matchSt) filtered.add(item);
        }
        myAuctionsTable.setItems(filtered);
    }

    // ── Create Listing tab ────────────────────────────────────
    private void setupCreateListingTab() {
        formCategory.getItems().addAll("Vehicles", "Watches", "Art", "Jewellery", "Electronics", "Other");
        formDuration.getItems().addAll("1 Day", "3 Days", "7 Days", "14 Days", "30 Days");
        formCondition.getItems().addAll("New", "Like New", "Excellent", "Good", "Fair");

        formCategory.getSelectionModel().selectFirst();
        formDuration.getSelectionModel().selectFirst();
        formCondition.getSelectionModel().selectFirst();

        // Live preview bindings
        formItemName.textProperty().addListener((o, old, v) -> previewTitle.setText(v.isEmpty() ? "Item Name" : v));
        formCategory.setOnAction(e -> {
            String cat = formCategory.getValue();
            previewCategory.setText(cat != null ? cat : "Category");
            previewEmoji.setText(emojiFor(cat));
        });
        formStartPrice.textProperty().addListener((o, old, v) ->
            previewPrice.setText(v.isEmpty() ? "$—" : "$" + v));
        formReservePrice.textProperty().addListener((o, old, v) ->
            previewReserve.setText(v.isEmpty() ? "$—" : "$" + v));
        formDuration.setOnAction(e -> {
            String dur = formDuration.getValue();
            previewDuration.setText(dur != null ? dur : "—");
        });
    }

    @FXML
    private void handleSubmitListing() {
        formMessage.setVisible(false);

        String name     = formItemName.getText().trim();
        String cat      = formCategory.getValue();
        String startStr = formStartPrice.getText().trim();
        String desc     = formDescription.getText().trim();

        if (name.isEmpty() || cat == null || startStr.isEmpty()) {
            showFormMessage("Please fill in Item Name, Category, and Starting Price.", false);
            return;
        }

        double startPrice;
        try {
            startPrice = Double.parseDouble(startStr.replace(",", ""));
        } catch (NumberFormatException e) {
            showFormMessage("Starting price must be a valid number.", false);
            return;
        }

        // Mock submission — in real app send to server
        LocalDateTime now = LocalDateTime.now();
        AuctionItem newItem = new AuctionItem(
            myAuctions.size() + 100L, myAuctions.size() + 200L,
            UserSession.getInstance().getCurrentUser().getId(),
            UserSession.getInstance().getCurrentUser().getUsername(),
            name, desc.isEmpty() ? cat.toUpperCase() : desc,
            cat, "PENDING",
            startPrice, startPrice,
            now, now.plusDays(7),
            null, 0
        );
        myAuctions.add(newItem);
        updateStatCards();
        updateSidebarStats();

        showFormMessage("✅ Listing submitted for admin approval!", true);
        handleClearForm();
        System.out.println("✅ New listing submitted: " + name);
    }

    @FXML
    private void handleClearForm() {
        formItemName.clear();
        formStartPrice.clear();
        formReservePrice.clear();
        formDescription.clear();
        formCategory.getSelectionModel().selectFirst();
        formDuration.getSelectionModel().selectFirst();
        formCondition.getSelectionModel().selectFirst();
        previewTitle.setText("Item Name");
        previewCategory.setText("Category");
        previewPrice.setText("$—");
        previewReserve.setText("$—");
        previewDuration.setText("—");
        previewEmoji.setText("⭐");
    }

    @FXML
    private void handleCreateListing() {
        // Switch to Create Listing tab (index 1)
        // Would need fx:id on TabPane — handled via sidebar button
    }

    private void showFormMessage(String msg, boolean success) {
        formMessage.setText(msg);
        formMessage.setStyle(success
            ? "-fx-text-fill: #4ade80; -fx-font-size: 12px;"
            : "-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        formMessage.setVisible(true);
    }

    // ── Bids Received tab ─────────────────────────────────────
    private void setupBidsTab() {
        colBidItem  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colBidder   .setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colBidAmount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        colBidTime  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        colBidStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));
        bidsReceivedTable.setItems(bidsData);
    }

    // ── Mock data ─────────────────────────────────────────────
    private void loadMockData() {
        LocalDateTime now = LocalDateTime.now();
        myAuctions.addAll(List.of(
            new AuctionItem(1L, 101L,
                UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getCurrentUser().getId() : 2L,
                "Seller Test",
                "Pioneer Zenith Hybrid", "LIMITED PRODUCTION 1 OF 50",
                "Vehicles", "RUNNING",
                180000, 245000,
                now.minusHours(2), now.plusSeconds(7685), null, 47),
            new AuctionItem(3L, 103L,
                UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getCurrentUser().getId() : 2L,
                "Seller Test",
                "Ethereal Horizon", "MIXED MEDIA ON CANVAS (2024)",
                "Art", "RUNNING",
                12000, 18900,
                now.minusHours(1), now.plusSeconds(31332), null, 12),
            new AuctionItem(7L, 107L,
                UserSession.getInstance().isLoggedIn() ? UserSession.getInstance().getCurrentUser().getId() : 2L,
                "Seller Test",
                "Sapphire Ring 3ct", "VVS1 CERTIFIED",
                "Jewellery", "PENDING",
                10000, 10000,
                now.plusDays(1), now.plusDays(8), null, 0)
        ));

        bidsData.addAll(List.of(
            new String[]{"Pioneer Zenith Hybrid", "@bidder_99",    "$245,000", "14:32:01", "Winning"},
            new String[]{"Pioneer Zenith Hybrid", "@luxcollector", "$240,000", "14:28:44", "Outbid"},
            new String[]{"Ethereal Horizon",      "@marcus_g",     "$18,900",  "13:55:12", "Winning"},
            new String[]{"Ethereal Horizon",      "@artlover22",   "$17,500",  "13:40:08", "Outbid"}
        ));

        updateStatCards();
        updateSidebarStats();
    }

    private void updateStatCards() {
        long active  = myAuctions.stream().filter(AuctionItem::isRunning).count();
        long pending = myAuctions.stream().filter(AuctionItem::isPending).count();
        int  bids    = myAuctions.stream().mapToInt(AuctionItem::getTotalBids).sum();
        double rev   = myAuctions.stream()
            .filter(AuctionItem::isClosed)
            .mapToDouble(AuctionItem::getCurrentPrice).sum();

        if (cardActive    != null) cardActive.setText(String.valueOf(active));
        if (cardTotalBids != null) cardTotalBids.setText(String.valueOf(bids));
        if (cardRevenue   != null) cardRevenue.setText(String.format("$%,.0f", rev));
        if (cardPending   != null) cardPending.setText(String.valueOf(pending));
    }

    private void updateSidebarStats() {
        long active  = myAuctions.stream().filter(AuctionItem::isRunning).count();
        long pending = myAuctions.stream().filter(AuctionItem::isPending).count();
        int  bids    = myAuctions.stream().mapToInt(AuctionItem::getTotalBids).sum();

        if (quickActiveLabel  != null) quickActiveLabel.setText(active  + " Active Auctions");
        if (quickPendingLabel != null) quickPendingLabel.setText(pending + " Pending Approval");
        if (quickBidsLabel    != null) quickBidsLabel.setText(bids    + " Bids Received");
    }

    // ── Logout ────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        NavigationUtils.logout();
    }

    // ── Utilities ─────────────────────────────────────────────
    private String formatTime(int seconds) {
        if (seconds <= 0) return "Ended";
        return String.format("%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private String emojiFor(String category) {
        if (category == null) return "⭐";
        return switch (category.toLowerCase()) {
            case "vehicles"            -> "🏎️";
            case "watches"             -> "⌚";
            case "art"                 -> "🖼️";
            case "jewellery","jewelry" -> "💎";
            case "electronics"         -> "💻";
            default                    -> "⭐";
        };
    }
}