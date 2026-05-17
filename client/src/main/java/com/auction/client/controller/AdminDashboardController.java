package com.auction.client.controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.auction.client.model.AuctionItem;
import com.auction.client.model.User;
import com.auction.client.util.NavigationUtils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;

public class AdminDashboardController {

    // ── Sidebar ───────────────────────────────────────────────
    @FXML private Button sideOverview;
    @FXML private Button sideUsers;
    @FXML private Button sideAuctions;
    @FXML private Button sideApprovals;
    @FXML private Button sideReports;
    @FXML private Button sideSettings;
    @FXML private Circle dotBidEngine;

    // ── Stat cards ────────────────────────────────────────────
    @FXML private Label cardUsers;
    @FXML private Label cardAuctions;
    @FXML private Label cardRevenue;
    @FXML private Label cardPending;
    @FXML private Label serverTimeLabel;

    // ── TabPane ───────────────────────────────────────────────
    @FXML private javafx.scene.control.TabPane tabPane;

    // ── Users tab ─────────────────────────────────────────────
    @FXML private TextField          searchUsers;
    @FXML private ComboBox<String>   filterRole;
    @FXML private TableView<User>    usersTable;
    @FXML private TableColumn<User, String> colUserId;
    @FXML private TableColumn<User, String> colUserName;
    @FXML private TableColumn<User, String> colUserEmail;
    @FXML private TableColumn<User, String> colUserRole;
    @FXML private TableColumn<User, String> colUserStatus;
    @FXML private TableColumn<User, String> colUserJoined;
    @FXML private TableColumn<User, String> colUserAction;

    // ── Auctions tab ──────────────────────────────────────────
    @FXML private TextField                  searchAuctions;
    @FXML private ComboBox<String>           filterAuctionStatus;
    @FXML private TableView<AuctionItem>     auctionsTable;
    @FXML private TableColumn<AuctionItem, String> colAucId;
    @FXML private TableColumn<AuctionItem, String> colAucTitle;
    @FXML private TableColumn<AuctionItem, String> colAucSeller;
    @FXML private TableColumn<AuctionItem, String> colAucBid;
    @FXML private TableColumn<AuctionItem, String> colAucBids;
    @FXML private TableColumn<AuctionItem, String> colAucStatus;
    @FXML private TableColumn<AuctionItem, String> colAucAction;

    // ── Approvals tab ─────────────────────────────────────────
    @FXML private TableView<AuctionItem>     approvalsTable;
    @FXML private TableColumn<AuctionItem, String> colAppItem;
    @FXML private TableColumn<AuctionItem, String> colAppSeller;
    @FXML private TableColumn<AuctionItem, String> colAppCategory;
    @FXML private TableColumn<AuctionItem, String> colAppPrice;
    @FXML private TableColumn<AuctionItem, String> colAppDate;
    @FXML private TableColumn<AuctionItem, String> colAppAction;

    // ── Data ──────────────────────────────────────────────────
    private final ObservableList<User>        allUsers    = FXCollections.observableArrayList();
    private final ObservableList<AuctionItem> allAuctions = FXCollections.observableArrayList();
    private final ObservableList<AuctionItem> pendingItems= FXCollections.observableArrayList();
    private Timer clockTimer;

    private static final DateTimeFormatter UTC_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Lifecycle ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        loadMockData();
        setupSidebar();
        setupUsersTab();
        setupAuctionsTab();
        setupApprovalsTab();
        updateStatCards();
        startClock();
    }

    // ── Sidebar ───────────────────────────────────────────────
    private void setupSidebar() {
        // Switch tabs on sidebar click
        sideOverview .setOnAction(e -> { tabPane.getSelectionModel().select(0); setActive(sideOverview);  });
        sideUsers    .setOnAction(e -> { tabPane.getSelectionModel().select(0); setActive(sideUsers);     });
        sideAuctions .setOnAction(e -> { tabPane.getSelectionModel().select(1); setActive(sideAuctions);  });
        sideApprovals.setOnAction(e -> { tabPane.getSelectionModel().select(2); setActive(sideApprovals); });
        sideReports  .setOnAction(e -> setActive(sideReports));
        sideSettings .setOnAction(e -> setActive(sideSettings));
    }

    private void setActive(Button active) {
        List.of(sideOverview, sideUsers, sideAuctions, sideApprovals, sideReports, sideSettings)
            .forEach(b -> {
                b.getStyleClass().remove("al-nav-item-active");
                if (!b.getStyleClass().contains("al-nav-item"))
                    b.getStyleClass().add("al-nav-item");
            });
        active.getStyleClass().remove("al-nav-item");
        if (!active.getStyleClass().contains("al-nav-item-active"))
            active.getStyleClass().add("al-nav-item-active");
    }

    // ── Users tab ─────────────────────────────────────────────
    private void setupUsersTab() {
        filterRole.getItems().addAll("All Roles", "ADMIN", "SELLER", "BIDDER");
        filterRole.getSelectionModel().selectFirst();

        colUserId    .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colUserName  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colUserEmail .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colUserRole  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        colUserStatus.setCellValueFactory(d -> new SimpleStringProperty("Active"));
        colUserJoined.setCellValueFactory(d -> new SimpleStringProperty("2026-01-01"));
        colUserAction.setCellValueFactory(d -> new SimpleStringProperty("Ban / Edit"));

        usersTable.setItems(allUsers);

        // Search
        searchUsers.textProperty().addListener((obs, old, val) -> applyUserFilter());
        filterRole.setOnAction(e -> applyUserFilter());
    }

    private void applyUserFilter() {
        String kw   = searchUsers.getText().trim().toLowerCase();
        String role = filterRole.getValue();

        ObservableList<User> filtered = FXCollections.observableArrayList();
        for (User u : allUsers) {
            boolean matchKw   = kw.isEmpty()
                || u.getUsername().toLowerCase().contains(kw)
                || u.getEmail().toLowerCase().contains(kw);
            boolean matchRole = "All Roles".equals(role)
                || u.getRole().equalsIgnoreCase(role);
            if (matchKw && matchRole) filtered.add(u);
        }
        usersTable.setItems(filtered);
    }

    // ── Auctions tab ──────────────────────────────────────────
    private void setupAuctionsTab() {
        filterAuctionStatus.getItems().addAll("All", "Live", "Ending Soon", "Pending", "Closed");
        filterAuctionStatus.getSelectionModel().selectFirst();

        colAucId    .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getAuctionId())));
        colAucTitle .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        colAucSeller.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSellerName()));
        colAucBid   .setCellValueFactory(d -> new SimpleStringProperty(String.format("$%,.0f", d.getValue().getCurrentPrice())));
        colAucBids  .setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getTotalBids())));
        colAucStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayStatus()));
        colAucAction.setCellValueFactory(d -> new SimpleStringProperty("End / Remove"));

        auctionsTable.setItems(allAuctions);

        searchAuctions.textProperty().addListener((obs, old, val) -> applyAuctionFilter());
        filterAuctionStatus.setOnAction(e -> applyAuctionFilter());
    }

    private void applyAuctionFilter() {
        String kw     = searchAuctions.getText().trim().toLowerCase();
        String status = filterAuctionStatus.getValue();

        ObservableList<AuctionItem> filtered = FXCollections.observableArrayList();
        for (AuctionItem item : allAuctions) {
            boolean matchKw = kw.isEmpty()
                || item.getItemName().toLowerCase().contains(kw)
                || item.getSellerName().toLowerCase().contains(kw);
            boolean matchSt = "All".equals(status)
                || (status.equals("Live")         && item.isRunning() && !item.isEndingSoon())
                || (status.equals("Ending Soon")  && item.isEndingSoon())
                || (status.equals("Pending")      && item.isPending())
                || (status.equals("Closed")       && item.isClosed());
            if (matchKw && matchSt) filtered.add(item);
        }
        auctionsTable.setItems(filtered);
    }

    // ── Approvals tab ─────────────────────────────────────────
    private void setupApprovalsTab() {
        colAppItem    .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getItemName()));
        colAppSeller  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSellerName()));
        colAppCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        colAppPrice   .setCellValueFactory(d -> new SimpleStringProperty(String.format("$%,.0f", d.getValue().getCurrentPrice())));
        colAppDate    .setCellValueFactory(d -> new SimpleStringProperty("2026-05-16"));
        colAppAction  .setCellValueFactory(d -> new SimpleStringProperty("✅ Approve / ❌ Reject"));

        approvalsTable.setItems(pendingItems);

        // Row click to approve/reject via dialog simulation
        approvalsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                AuctionItem selected = approvalsTable.getSelectionModel().getSelectedItem();
                if (selected != null) handleApprove(selected);
            }
        });
    }

    private void handleApprove(AuctionItem item) {
        // Mock approve — remove from pending, update stat
        pendingItems.remove(item);
        updateStatCards();
        System.out.println("✅ Approved: " + item.getItemName());
    }

    // ── Stat cards ────────────────────────────────────────────
    private void updateStatCards() {
        long live    = allAuctions.stream().filter(AuctionItem::isRunning).count();
        long pending = pendingItems.size();
        double rev   = allAuctions.stream()
            .filter(AuctionItem::isClosed)
            .mapToDouble(AuctionItem::getCurrentPrice).sum();

        if (cardUsers    != null) cardUsers.setText(String.valueOf(allUsers.size()));
        if (cardAuctions != null) cardAuctions.setText(String.valueOf(live));
        if (cardRevenue  != null) cardRevenue.setText(rev > 0
            ? String.format("$%,.0f", rev) : "$2.4M");     // fallback for demo
        if (cardPending  != null) cardPending.setText(String.valueOf(pending));
    }

    // ── Server clock ──────────────────────────────────────────
    private void startClock() {
        clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> {
                    String utc = LocalDateTime.now(ZoneOffset.UTC).format(UTC_FMT);
                    if (serverTimeLabel != null)
                        serverTimeLabel.setText("Server: " + utc + " UTC");
                });
            }
        }, 0, 1000);
    }

    // ── Logout ────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        if (clockTimer != null) clockTimer.cancel();
        NavigationUtils.logout();
    }

    // ── Export ────────────────────────────────────────────────
    @FXML
    private void handleExportUsers() {
        System.out.println("📥 Exporting users CSV... (" + allUsers.size() + " users)");
        // TODO: write to file when server layer is ready
    }

    // ── Mock data ─────────────────────────────────────────────
    private void loadMockData() {
        LocalDateTime now = LocalDateTime.now();

        // Users
        allUsers.addAll(List.of(
            new User(1L,  "@collector_a",   "collector@aureate.com",   "BIDDER"),
            new User(2L,  "@sterlinghouse", "sterling@aureate.com",    "SELLER"),
            new User(3L,  "@marcus_gold",   "marcus@aureate.com",      "SELLER"),
            new User(4L,  "@bidder_99",     "bidder99@gmail.com",      "BIDDER"),
            new User(5L,  "@luxcollector",  "lux@gmail.com",           "BIDDER"),
            new User(6L,  "@artlover22",    "artlover@gmail.com",      "BIDDER"),
            new User(99L, "admin",          "admin@auctionpro.com",    "ADMIN")
        ));

        // Auctions
        allAuctions.addAll(List.of(
            new AuctionItem(1L, 101L, 2L, "@sterlinghouse",
                "Pioneer Zenith Hybrid", "LIMITED PRODUCTION 1 OF 50",
                "Vehicles", "RUNNING", 180000, 245000,
                now.minusHours(2), now.plusSeconds(7685), null, 47),
            new AuctionItem(2L, 102L, 3L, "@marcus_gold",
                "Vanguard Tourbillon", "ROSE GOLD SKELETON EDITION",
                "Watches", "RUNNING", 70000, 82400,
                now.minusHours(5), now.plusSeconds(704), null, 31),
            new AuctionItem(3L, 103L, 2L, "@sterlinghouse",
                "Ethereal Horizon", "MIXED MEDIA ON CANVAS (2024)",
                "Art", "RUNNING", 12000, 18900,
                now.minusHours(1), now.plusSeconds(31332), null, 12),
            new AuctionItem(4L, 104L, 3L, "@marcus_gold",
                "Wraith Stealth Tender", "CUSTOM CARBON SERIES",
                "Vehicles", "RUNNING", 400000, 512000,
                now.minusHours(3), now.plusDays(1), null, 28),
            new AuctionItem(5L, 105L, 2L, "@sterlinghouse",
                "Neon Phantom", "DIGITAL ART 1/1 EDITION",
                "Art", "RUNNING", 6000, 9500,
                now.minusHours(1), now.plusSeconds(5400), null, 15),
            new AuctionItem(6L, 106L, 3L, "@marcus_gold",
                "Quantum X Laptop", "TITANIUM EDITION 2024",
                "Electronics", "RUNNING", 3000, 4200,
                now.minusHours(4), now.plusSeconds(500), null, 8)
        ));

        // Pending approvals
        pendingItems.addAll(List.of(
            new AuctionItem(7L,  107L, 2L, "@sterlinghouse",
                "Sapphire Ring 3ct", "VVS1 CERTIFIED",
                "Jewellery", "PENDING", 10000, 10000,
                now.plusDays(1), now.plusDays(8), null, 0),
            new AuctionItem(8L,  108L, 3L, "@marcus_gold",
                "Rolex Daytona 2024", "STAINLESS STEEL OYSTERFLEX",
                "Watches", "PENDING", 25000, 25000,
                now.plusDays(2), now.plusDays(9), null, 0),
            new AuctionItem(9L,  109L, 2L, "@sterlinghouse",
                "Ferrari 488 Spider", "2019 LOW MILEAGE",
                "Vehicles", "PENDING", 280000, 280000,
                now.plusDays(1), now.plusDays(7), null, 0)
        ));
    }
}