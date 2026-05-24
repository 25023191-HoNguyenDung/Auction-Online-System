package com.auction.client.controller;

import java.time.LocalDateTime;

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
    @FXML private Label  pageSubtitleLabel;
    @FXML private Label  quickActiveLabel;
    @FXML private Label  quickPendingLabel;
    @FXML private Label  quickBidsLabel;
    @FXML private Button sideOverview;
    @FXML private Button sideMyAuctions;
    @FXML private Button sideCreate;
    @FXML private Button sideBids;
    @FXML private Button sideHistory;

    @FXML private javafx.scene.control.TabPane tabPane;

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
    @FXML private ComboBox<String> formDuration;
    @FXML private ComboBox<String> formCondition;
    @FXML private TextArea         formDescription;
    @FXML private Label            formMessage;

    // Preview
    @FXML private Label previewTitle;
    @FXML private Label previewCategory;
    @FXML private Label previewPrice;
    @FXML private Label previewDuration;
    @FXML private Label previewEmoji;

    // ── Bids Received tab ─────────────────────────────────────
    @FXML private TableView<String[]>              bidsReceivedTable;
    @FXML private TableColumn<String[], String>    colBidItem;
    @FXML private TableColumn<String[], String>    colBidder;
    @FXML private TableColumn<String[], String>    colBidAmount;
    @FXML private TableColumn<String[], String>    colBidTime;
    @FXML private TableColumn<String[], String>    colBidStatus;

    // History tab
    @FXML private TableView<String[]>              historyTable;
    @FXML private TableColumn<String[], String>    colHistoryItem;
    @FXML private TableColumn<String[], String>    colHistoryAction;
    @FXML private TableColumn<String[], String>    colHistoryAmount;
    @FXML private TableColumn<String[], String>    colHistoryTime;
    @FXML private TableColumn<String[], String>    colHistoryStatus;

    // ── Mock data ─────────────────────────────────────────────
    private final ObservableList<AuctionItem> myAuctions = FXCollections.observableArrayList();
    private final ObservableList<String[]>    bidsData   = FXCollections.observableArrayList();
    private final ObservableList<String[]>    historyData = FXCollections.observableArrayList();
    private java.util.Timer refreshTimer;

    // ── Lifecycle ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        loadSellerInfo();
        setupSidebarButtons();
        setupMyAuctionsTab();
        setupCreateListingTab();
        setupBidsTab();
        setupHistoryTab();
        refreshSellerAuctions();
        startRefreshTimer();
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
        sideOverview  .setOnAction(e -> { tabPane.getSelectionModel().select(0); setActive(sideOverview);   refreshSellerAuctions(); });
        sideMyAuctions.setOnAction(e -> { tabPane.getSelectionModel().select(0); setActive(sideMyAuctions); refreshSellerAuctions(); });
        sideCreate    .setOnAction(e -> { tabPane.getSelectionModel().select(1); setActive(sideCreate);     });
        sideBids      .setOnAction(e -> { tabPane.getSelectionModel().select(2); setActive(sideBids);       refreshSellerAuctions(); });
        sideHistory   .setOnAction(e -> { tabPane.getSelectionModel().select(3); setActive(sideHistory);    refreshSellerAuctions(); });
    }

    private void setActive(Button active) {
        java.util.List.of(sideOverview, sideMyAuctions, sideCreate, sideBids, sideHistory)
            .forEach(b -> {
                b.getStyleClass().remove("al-nav-item-active");
                if (!b.getStyleClass().contains("al-nav-item"))
                    b.getStyleClass().add("al-nav-item");
            });
        active.getStyleClass().remove("al-nav-item");
        if (!active.getStyleClass().contains("al-nav-item-active"))
            active.getStyleClass().add("al-nav-item-active");
    }

    // ── My Auctions tab ───────────────────────────────────────
    private void setupMyAuctionsTab() {
        filterMyStatus.getItems().addAll("All", "Live", "Ending Soon", "Upcoming", "Closed");
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

        // Action buttons
        colMyAction.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            private final Button endBtn  = makeBtn("End",  "#f59e0b", "rgba(245,158,11,0.15)");
            private final Button editBtn = makeBtn("Edit", "#f0b429", "rgba(240,180,41,0.15)");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, endBtn, editBtn);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                endBtn.setOnAction(e -> {
                    AuctionItem item = getTableView().getItems().get(getIndex());
                    handleEndMyAuction(item);
                });
                editBtn.setOnAction(e -> {
                    AuctionItem item = getTableView().getItems().get(getIndex());
                    handleEditMyAuction(item);
                });
            }
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

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
                || (status.equals("Live")        && item.isRunning())
                || (status.equals("Ending Soon") && item.isEndingSoon())
                || (status.equals("Upcoming")    && item.isPending())
                || (status.equals("Closed")      && item.isClosed());
            if (matchKw && matchSt) filtered.add(item);
        }
        myAuctionsTable.setItems(filtered);
    }

    // ── Create Listing tab ────────────────────────────────────
    private void setupCreateListingTab() {
        formCategory.getItems().addAll("Vehicles", "Watches", "Art", "Electronics", "Other");
        formDuration.getItems().addAll(
            "1 Minute", "3 Minutes", "5 Minutes", "10 Minutes", "30 Minutes", "60 Minutes"
        );
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

        showFormMessage("Submitting listing to server...", true);

        new Thread(() -> {
            try {
                java.util.concurrent.CompletableFuture<com.auction.common.protocol.MessageEnvelope> responseFuture = new java.util.concurrent.CompletableFuture<>();
                com.auction.client.network.ClientMessageSender sender = new com.auction.client.network.ClientMessageSender();
                long sellerId = UserSession.getInstance().getCurrentUser().getId();
                
                int minutes = 5;
                String durVal = formDuration.getValue();
                if (durVal != null) {
                    minutes = switch (durVal) {
                        case "1 Minute" -> 1;
                        case "3 Minutes" -> 3;
                        case "5 Minutes" -> 5;
                        case "10 Minutes" -> 10;
                        case "30 Minutes" -> 30;
                        case "60 Minutes" -> 60;
                        default -> 5;
                    };
                }

                String messageId = sender.sendSubmitListing(
                    sellerId,
                    name,
                    desc.isEmpty() ? cat.toUpperCase() : desc,
                    cat,
                    startPrice,
                    minutes
                );
                
                com.auction.client.network.ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);
                
                com.auction.common.protocol.MessageEnvelope resEnvelope = responseFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                com.auction.common.protocol.SubmitListingResPayload res = new com.auction.common.protocol.ProtocolMapper().parsePayload(resEnvelope, com.auction.common.protocol.SubmitListingResPayload.class);
                
                javafx.application.Platform.runLater(() -> {
                    if (res.isSuccess()) {
                        refreshSellerAuctions();
                        addHistory(name, "Created listing", String.format("$%,.0f", startPrice), "Pending Review");
                        showFormMessage("✅ Listing submitted for admin approval!", true);
                        handleClearForm();
                    } else {
                        showFormMessage("❌ Failed to submit listing: " + res.getMessage(), false);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showFormMessage("❌ Network error: " + e.getMessage(), false);
                });
            }
        }).start();
    }

    @FXML
    private void handleClearForm() {
        formItemName.clear();
        formStartPrice.clear();
        formDescription.clear();
        formCategory.getSelectionModel().selectFirst();
        formDuration.getSelectionModel().selectFirst();
        formCondition.getSelectionModel().selectFirst();
        previewTitle.setText("Item Name");
        previewCategory.setText("Category");
        previewPrice.setText("$—");
        previewDuration.setText("—");
        previewEmoji.setText("⭐");
    }

    @FXML
    private void handleCreateListing() {
        tabPane.getSelectionModel().select(1);
        setActive(sideCreate);
    }

    private void handleEndMyAuction(AuctionItem item) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("End Auction");
        alert.setHeaderText(null);
        alert.setContentText("End \"" + item.getItemName() + "\" early?\nThis cannot be undone.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                new Thread(() -> {
                    try {
                        java.util.concurrent.CompletableFuture<com.auction.common.protocol.MessageEnvelope> responseFuture = new java.util.concurrent.CompletableFuture<>();
                        com.auction.client.network.ClientMessageSender sender = new com.auction.client.network.ClientMessageSender();
                        String messageId = sender.sendAdminAction(item.getAuctionId(), "END");
                        com.auction.client.network.ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);
                        
                        com.auction.common.protocol.MessageEnvelope resEnvelope = responseFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                        com.auction.common.protocol.AdminActionResPayload res = new com.auction.common.protocol.ProtocolMapper().parsePayload(resEnvelope, com.auction.common.protocol.AdminActionResPayload.class);
                        
                        javafx.application.Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                refreshSellerAuctions();
                                addHistory(item.getItemName(), "Ended auction", String.format("$%,.0f", item.getCurrentPrice()), "Closed");
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    private void handleEditMyAuction(AuctionItem item) {
        // Pre-fill the Create Listing form with this item's data
        formItemName.setText(item.getItemName());
        formCategory.setValue(item.getCategory());
        formStartPrice.setText(String.format("%.0f", item.getCurrentPrice()));
        formDescription.setText(item.getDescription() != null ? item.getDescription() : "");

        // Switch to Create Listing tab
        tabPane.getSelectionModel().select(1);
        setActive(sideCreate);

        showFormMessage("Editing: " + item.getItemName() + " — submit to update.", true);
    }

    private Button makeBtn(String text, String textColor, String bgColor) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color:" + bgColor + ";" +
            "-fx-text-fill:" + textColor + ";" +
            "-fx-font-size:10px; -fx-font-family:'Arial'; -fx-font-weight:bold;" +
            "-fx-border-color:" + textColor + "; -fx-border-radius:4;" +
            "-fx-background-radius:4; -fx-padding:3 8; -fx-cursor:hand;");
        return btn;
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

    private void setupHistoryTab() {
        colHistoryItem  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        colHistoryAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        colHistoryAmount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        colHistoryTime  .setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        colHistoryStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));
        historyTable.setItems(historyData);
    }

    private void addHistory(String item, String action, String amount, String status) {
        historyData.add(0, new String[]{
            item,
            action,
            amount,
            java.time.LocalTime.now().withNano(0).toString(),
            status
        });
    }

    private void refreshSellerAuctions() {
        new Thread(() -> {
            try {
                java.util.concurrent.CompletableFuture<com.auction.common.protocol.MessageEnvelope> responseFuture = new java.util.concurrent.CompletableFuture<>();
                com.auction.client.network.ClientMessageSender sender = new com.auction.client.network.ClientMessageSender();
                long userId = UserSession.getInstance().isLoggedIn() 
                    ? UserSession.getInstance().getCurrentUser().getId() 
                    : 0L;
                String username = UserSession.getInstance().isLoggedIn()
                    ? UserSession.getInstance().getCurrentUser().getUsername()
                    : "";
                
                String messageId = sender.sendListAuctions(userId, 1, 100, null);
                com.auction.client.network.ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);
                
                com.auction.common.protocol.MessageEnvelope resEnvelope = responseFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                
                if (resEnvelope.getType() == com.auction.common.protocol.MessageType.LIST_AUCTIONS_RES) {
                    com.auction.common.protocol.ListAuctionsResPayload res = new com.auction.common.protocol.ProtocolMapper().parsePayload(resEnvelope, com.auction.common.protocol.ListAuctionsResPayload.class);
                    
                    java.util.List<AuctionItem> myItems = new java.util.ArrayList<>();
                    
                    class TempEntry {
                        LocalDateTime time;
                        String[] data;
                        TempEntry(LocalDateTime time, String[] data) {
                            this.time = time;
                            this.data = data;
                        }
                    }
                    java.util.List<TempEntry> tempBids = new java.util.ArrayList<>();
                    java.util.List<TempEntry> tempHistory = new java.util.ArrayList<>();
                    
                    for (com.auction.common.protocol.AuctionSummaryItem summary : res.getAuctions()) {
                        if (summary.getSellerId() == userId || (username != null && username.equalsIgnoreCase(summary.getSellerName()))) {
                            myItems.add(new AuctionItem(
                                summary.getAuctionId(), 
                                0L, 
                                summary.getSellerId(), 
                                summary.getSellerName(), 
                                summary.getItemName(), 
                                summary.getDescription(), 
                                summary.getCategory(), 
                                summary.getStatus(), 
                                summary.getCurrentHighestBid().doubleValue(), 
                                summary.getCurrentHighestBid().doubleValue(), 
                                LocalDateTime.now().minusMinutes(5), 
                                LocalDateTime.ofInstant(summary.getEndTime(), java.time.ZoneId.systemDefault()), 
                                null, 
                                summary.getBidHistory() != null ? summary.getBidHistory().size() : 0, 
                                summary.getBidHistory()
                            ));
                            
                            String itemName = summary.getItemName();
                            String auctionStatus = summary.getStatus();
                            LocalDateTime endTime = LocalDateTime.ofInstant(summary.getEndTime(), java.time.ZoneId.systemDefault());

                            // 2. Bids Received & Bid History
                            java.util.List<String> history = summary.getBidHistory();
                            
                            LocalDateTime createTime = null;
                            if (summary.getStartTime() != null) {
                                try {
                                    createTime = LocalDateTime.ofInstant(summary.getStartTime(), java.time.ZoneId.systemDefault());
                                } catch (Exception ex) {}
                            }
                            if (createTime == null) {
                                if (endTime.isAfter(LocalDateTime.now())) {
                                    createTime = endTime.minusHours(2).minusMinutes(5);
                                } else {
                                    createTime = endTime.minusMinutes(5);
                                }
                            }
                            if (createTime.isAfter(LocalDateTime.now())) {
                                createTime = LocalDateTime.now().minusMinutes(5);
                            }
                            
                            String createTimeStr = createTime.toLocalTime().withNano(0).toString();
                            String createStatus = "OPEN".equalsIgnoreCase(auctionStatus) || "PENDING".equalsIgnoreCase(auctionStatus) 
                                ? "Pending Review" : "Approved";
                            tempHistory.add(new TempEntry(createTime, new String[]{
                                itemName,
                                "Created listing",
                                String.format("$%,.0f", summary.getCurrentHighestBid().doubleValue()),
                                createTimeStr,
                                createStatus
                            }));

                            if (history != null && !history.isEmpty()) {
                                for (int i = 0; i < history.size(); i++) {
                                    String entry = history.get(i);
                                    String[] parts = entry.split("  →  ");
                                    if (parts.length > 1) {
                                        String bidderName = parts[0].trim();
                                        String amtStr = parts[1].trim();

                                        LocalDateTime bidTime = null;
                                        if (parts.length > 2) {
                                            try {
                                                bidTime = LocalDateTime.parse(parts[2].trim());
                                            } catch (Exception ex) {}
                                        }
                                        if (bidTime == null) {
                                            if (endTime.isAfter(LocalDateTime.now())) {
                                                bidTime = endTime.minusHours(2).minusSeconds(10 * (i + 1));
                                            } else {
                                                bidTime = endTime.minusSeconds(10 * (i + 1));
                                            }
                                        }
                                        if (bidTime.isAfter(LocalDateTime.now())) {
                                            bidTime = LocalDateTime.now().minusSeconds(10 * (i + 1));
                                        }
                                        String bidTimeStr = bidTime.toLocalTime().withNano(0).toString();

                                        // Determine status
                                        String status;
                                        if (i == 0) { // highest bid
                                            if ("FINISHED".equalsIgnoreCase(auctionStatus) || "PAID".equalsIgnoreCase(auctionStatus) || "CLOSED".equalsIgnoreCase(auctionStatus)) {
                                                status = "Won";
                                            } else {
                                                status = "Winning";
                                            }
                                        } else {
                                            status = "Outbid";
                                        }

                                        tempBids.add(new TempEntry(bidTime, new String[]{
                                            itemName,
                                            bidderName,
                                            amtStr,
                                            bidTimeStr,
                                            status
                                        }));

                                        tempHistory.add(new TempEntry(bidTime, new String[]{
                                            itemName,
                                            "Bid from " + bidderName,
                                            amtStr,
                                            bidTimeStr,
                                            status
                                        }));
                                    }
                                }
                            }

                            // 3. History: Ended Auction event if closed/finished/paid
                            if ("FINISHED".equalsIgnoreCase(auctionStatus) || "PAID".equalsIgnoreCase(auctionStatus) || "CLOSED".equalsIgnoreCase(auctionStatus)) {
                                String endStatus = "Closed";
                                if (history != null && !history.isEmpty()) {
                                    String entry = history.get(0);
                                    String[] parts = entry.split("  →  ");
                                    if (parts.length > 1) {
                                        endStatus = "Winner: " + parts[0].trim();
                                    }
                                }
                                LocalDateTime actualEndTime = endTime;
                                if (actualEndTime.isAfter(LocalDateTime.now())) {
                                    actualEndTime = LocalDateTime.now();
                                }
                                tempHistory.add(new TempEntry(actualEndTime, new String[]{
                                    itemName,
                                    "Ended auction",
                                    String.format("$%,.0f", summary.getCurrentHighestBid().doubleValue()),
                                    actualEndTime.toLocalTime().withNano(0).toString(),
                                    endStatus
                                }));
                            }
                        }
                    }
                    
                    // Sort descending (latest/newest time first)
                    tempHistory.sort((a, b) -> b.time.compareTo(a.time));
                    tempBids.sort((a, b) -> b.time.compareTo(a.time));
                    
                    java.util.List<String[]> newHistoryList = new java.util.ArrayList<>();
                    for (TempEntry entry : tempHistory) {
                        newHistoryList.add(entry.data);
                    }
                    
                    java.util.List<String[]> newBidsList = new java.util.ArrayList<>();
                    for (TempEntry entry : tempBids) {
                        newBidsList.add(entry.data);
                    }
                    
                    javafx.application.Platform.runLater(() -> {
                        myAuctions.setAll(myItems);
                        bidsData.setAll(newBidsList);
                        historyData.setAll(newHistoryList);
                        applyAuctionFilter();
                        updateStatCards();
                        updateSidebarStats();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateStatCards() {
        long active  = myAuctions.stream().filter(AuctionItem::isRunning).count();
        long pending = myAuctions.stream().filter(AuctionItem::isPending).count();
        int  bids    = myAuctions.stream().mapToInt(AuctionItem::getTotalBids).sum();
        double rev   = myAuctions.stream()
            .filter(item -> item.isClosed() && item.getTotalBids() > 0)
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
        if (refreshTimer != null) refreshTimer.cancel();
        NavigationUtils.logout();
    }
    // ── Utilities ─────────────────────────────────────────────
    private void startRefreshTimer() {
        refreshTimer = new java.util.Timer(true);
        refreshTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() {
                refreshSellerAuctions();
            }
        }, 3000, 3000);
    }

    private String formatTime(int seconds) {
        if (seconds <= 0) return "Ended";
        return String.format("%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private String emojiFor(String category) {
        if (category == null) return "⭐";
        return switch (category.toLowerCase()) {
            case "vehicles", "vehicle"            -> "🏎️";
            case "watches", "watch"               -> "⌚";
            case "art", "fine art"                -> "🖼️";
            case "jewellery","jewelry"            -> "💎";
            case "electronics"                    -> "💻";
            default                               -> "⭐";
        };
    }
}
