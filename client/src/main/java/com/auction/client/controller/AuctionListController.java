package com.auction.client.controller;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import com.auction.client.model.AuctionItem;
import com.auction.client.network.ClientMessageSender;
import com.auction.client.network.ServerEventListener;
import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;
import com.auction.client.viewmodel.AuctionListViewModel;
import com.auction.common.protocol.DepositResPayload;
import com.auction.common.protocol.ErrorPayload;
import com.auction.common.protocol.MessageEnvelope;
import com.auction.common.protocol.MessageType;
import com.auction.common.protocol.ProtocolMapper;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AuctionListController {

    private final AuctionListViewModel viewModel = new AuctionListViewModel();

    // ── Navbar ────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label     userInitialLabel;
    @FXML private Button    btnLogout;

    // ── Sidebar Filters ───────────────────────────────────────
    @FXML private CheckBox catVehicles;
    @FXML private CheckBox catArts;
    @FXML private CheckBox catWatches;
    @FXML private CheckBox catElectronics;
    @FXML private TextField priceMin;
    @FXML private TextField priceMax;
    @FXML private Button    statusLive;
    @FXML private Button    statusUpcoming;
    @FXML private Button    statusEndingSoon;
    @FXML private Button    applyFilterBtn;

    // ── Wallet widget ─────────────────────────────────────────
    @FXML private Label     walletBalanceLabel;
    @FXML private TextField depositAmountField;
    @FXML private Button    btnDeposit;
    @FXML private Button    btnDeposit1;   // this is the WITHDRAW button in the FXML

    // ── Main Content ──────────────────────────────────────────
    @FXML private Label            countLabel;
    @FXML private ComboBox<String> sortCombo;
    @FXML private GridPane         cardsGrid;

    private Timer clockTimer;
    private int listRefreshCounter = 0;

    // Balance listener reference (kept so we can remove it on cleanup)
    private Consumer<Double> balanceListener;

    // ── Lifecycle ─────────────────────────────────────────────
    // ─── THAY THẾ LẠI LUỒNG LOAD TRONG HÀM initialize() ────────
    @FXML
    public void initialize() {
        loadUserInfo();
        setupWallet();
        setupSearch();
        setupSortCombo();
        setupStatusButtons();
        setupApplyFilter();
        // CHẠY BẤT ĐỒNG BỘ: Tạo thread phụ để kết nối Socket không làm đơ giao diện chính
        new Thread(() -> {
            try {
                viewModel.loadData(); // Kết nối socket và tải dữ liệu thật từ DB
                
                // Trở lại UI Thread để hiển thị danh sách sản phẩm lên màn hình
                Platform.runLater(() -> {
                    refreshCards();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        startCountdownTimer();
    }

    // ── User info ─────────────────────────────────────────────
    private void loadUserInfo() {
        if (userInitialLabel == null) return;
        if (UserSession.getInstance().isLoggedIn()) {
            String name = UserSession.getInstance().getCurrentUser().getUsername();
            userInitialLabel.setText(
                name != null && !name.isEmpty()
                    ? String.valueOf(name.charAt(0)).toUpperCase() : "?");
        }
    }

    // ── Wallet ────────────────────────────────────────────────
    private void setupWallet() {
        // Show current balance immediately
        refreshWalletLabel();

        // Subscribe to balance changes from ANY screen (BidScreen, etc.)
        balanceListener = newBalance -> Platform.runLater(this::refreshWalletLabel);
        UserSession.getInstance().addBalanceListener(balanceListener);

        // Wire WITHDRAW button (fx:id="btnDeposit1" in FXML)
        if (btnDeposit1 != null) {
            btnDeposit1.setOnAction(e -> handleWithdrawSimulation());
        }
    }

    private void refreshWalletLabel() {
        if (walletBalanceLabel != null) {
            double bal = UserSession.getInstance().getBalance();
            walletBalanceLabel.setText(String.format("$%,.0f", bal));
            // Green when healthy, amber when low, red when very low
            if (bal >= 10_000) {
                walletBalanceLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 32px; -fx-font-weight: bold;");
            } else if (bal >= 1_000) {
                walletBalanceLabel.setStyle("-fx-text-fill: #f0b429; -fx-font-size: 32px; -fx-font-weight: bold;");
            } else {
                walletBalanceLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 32px; -fx-font-weight: bold;");
            }
        }
    }

    /** Called by FXML button fx:id="btnDeposit" */
    @FXML
    private void handleDepositSimulation() {
        String raw = depositAmountField != null
                ? depositAmountField.getText().trim().replace(",", "") : "";
        if (raw.isEmpty()) {
            showAlert("Please enter an amount.", false);
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            showAlert("Invalid amount — numbers only.", false);
            return;
        }
        if (amount <= 0) {
            showAlert("Amount must be greater than zero.", false);
            return;
        }
        // ← THAY ĐỔI CHÍNH: Gửi lên server thay vì chỉ update local
        if (btnDeposit != null) btnDeposit.setDisable(true);
        new Thread(() -> {
            try {
                java.util.concurrent.CompletableFuture<MessageEnvelope> responseFuture =
                        new java.util.concurrent.CompletableFuture<>();
                ClientMessageSender sender = new ClientMessageSender();
                long userId = UserSession.getInstance().getCurrentUser().getId();
                java.math.BigDecimal depositAmount = java.math.BigDecimal.valueOf(amount);
                String messageId = sender.sendDeposit(userId, depositAmount);
                ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);
                MessageEnvelope resEnvelope = responseFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                Platform.runLater(() -> {
                    if (btnDeposit != null) btnDeposit.setDisable(false);
                    if (resEnvelope.getType() == MessageType.ERROR_RES) {
                        try {
                            ErrorPayload err = new ProtocolMapper().parsePayload(resEnvelope, ErrorPayload.class);
                            showAlert("Nạp tiền thất bại: " + err.getMessage(), false);
                        } catch (Exception ex) {
                            showAlert("Nạp tiền thất bại.", false);
                        }
                    } else {
                        // Server xác nhận thành công → cập nhật số dư local theo số dư thật từ DB
                        try {
                            DepositResPayload res = new ProtocolMapper().parsePayload(resEnvelope, DepositResPayload.class);
                            // Call deposit to add the DEPOSIT transaction in history and notify
                            UserSession.getInstance().deposit(amount);
                            // Đồng bộ balance local = balance thật trong DB
                            double newBal = res.getNewBalance().doubleValue();
                            UserSession.getInstance().setBalance(newBal);
                        } catch (Exception ex) {
                            // fallback: cộng local nếu không parse được
                            UserSession.getInstance().deposit(amount);
                        }
                        if (depositAmountField != null) depositAmountField.clear();
                        showAlert(String.format("$%,.0f deposited successfully.", amount), true);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (btnDeposit != null) btnDeposit.setDisable(false);
                    showAlert("Network timeout. Please try again.", false);
                });
            }
        }).start();
    }

    /** Called by the WITHDRAW button (fx:id="btnDeposit1") */
    private void handleWithdrawSimulation() {
        String raw = depositAmountField != null ? depositAmountField.getText().trim().replace(",", "") : "";
        if (raw.isEmpty()) {
            showAlert("Please enter an amount.", false);
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            showAlert("Invalid amount — numbers only.", false);
            return;
        }
        if (amount <= 0) {
            showAlert("Amount must be greater than zero.", false);
            return;
        }
        try {
            UserSession.getInstance().withdraw(amount);
            if (depositAmountField != null) depositAmountField.clear();
            showAlert(String.format("$%,.0f withdrawn successfully.", amount), true);
        } catch (IllegalStateException ex) {
            showAlert("Insufficient balance.", false);
        } catch (Exception ex) {
            showAlert(ex.getMessage(), false);
        }
    }

    private void showAlert(String message, boolean success) {
        // Inline feedback — show in a small alert or reuse the deposit field's prompt
        if (walletBalanceLabel == null) return;
        // Use a temporary label trick: just print to console and rely on balance label update
        // For a richer UX, show a JavaFX alert
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
        alert.setTitle(success ? "Success" : "Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color:#161410; -fx-border-color:#2e2a1e;");
        alert.showAndWait();
    }

    // ── Logout ────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        cleanup();
        NavigationUtils.logout();
    }

    // ── History navigation ────────────────────────────────────
    @FXML
    private void handleNavHistory() {
        cleanup();
        NavigationUtils.navigateToBidHistory();
    }

    // ── Search ────────────────────────────────────────────────
    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> {
                viewModel.setKeyword(val);
                refreshCards();
            });
        }
    }

    // ── Sort ──────────────────────────────────────────────────
    private void setupSortCombo() {
        if (sortCombo == null) return;
        sortCombo.getItems().addAll(
            "Newest", "Price: Low to High", "Price: High to Low", "Ending Soon");
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setOnAction(e -> {
            String s = sortCombo.getValue();
            if (s == null) return;
            switch (s) {
                case "Price: Low to High" -> viewModel.setSortBy("PRICE_ASC");
                case "Price: High to Low" -> viewModel.setSortBy("PRICE_DESC");
                case "Ending Soon"        -> viewModel.setSortBy("ENDING_SOON");
                default                   -> viewModel.setSortBy("NEWEST");
            }
            refreshCards();
        });
    }

    // ── Status buttons ────────────────────────────────────────
    private void setupStatusButtons() {
        if (statusLive       != null) statusLive.setOnAction(e -> setActiveStatus(statusLive));
        if (statusUpcoming   != null) statusUpcoming.setOnAction(e -> setActiveStatus(statusUpcoming));
        if (statusEndingSoon != null) statusEndingSoon.setOnAction(e -> setActiveStatus(statusEndingSoon));
    }

    private void setActiveStatus(Button activeBtn) {
        boolean alreadyActive = activeBtn.getStyleClass().contains("active");

        statusLive.getStyleClass().remove("active");
        statusUpcoming.getStyleClass().remove("active");
        statusEndingSoon.getStyleClass().remove("active");

        if (alreadyActive) {
            viewModel.setFilterStatus("ALL");
        } else {
            activeBtn.getStyleClass().add("active");
            if      (activeBtn == statusLive)        viewModel.setFilterStatus("LIVE");
            else if (activeBtn == statusUpcoming)    viewModel.setFilterStatus("PENDING");
            else if (activeBtn == statusEndingSoon)  viewModel.setFilterStatus("ENDING_SOON");
        }
        refreshCards();
    }

    // ── Apply filters ─────────────────────────────────────────
    private void setupApplyFilter() {
        if (applyFilterBtn != null)
            applyFilterBtn.setOnAction(e -> handleApplyFilter());
    }

    @FXML
    private void handleApplyFilter() {
        double min = 0, max = Double.MAX_VALUE;
        try { min = Double.parseDouble(priceMin.getText().trim()); } catch (Exception ignored) {}
        try { max = Double.parseDouble(priceMax.getText().trim()); } catch (Exception ignored) {}
        viewModel.setPriceRange(min, max);

        java.util.Set<String> cats = new java.util.HashSet<>();
        if (catVehicles.isSelected())    { cats.add("Vehicles"); cats.add("Vehicle"); }
        if (catArts.isSelected())        { cats.add("Art"); cats.add("Fine Art"); }
        if (catWatches.isSelected())     { cats.add("Watches"); cats.add("Watch"); }
        if (catElectronics.isSelected()) { cats.add("Electronics"); }
        viewModel.setFilterCategories(cats);

        refreshCards();
    }

    // ── Cards ─────────────────────────────────────────────────
    private void refreshCards() {
        List<AuctionItem> items = viewModel.applyFilters();
        countLabel.setText("Showing " + items.size() + " of " + viewModel.getTotalCount() + " items");
        cardsGrid.getChildren().clear();

        int col = 0, row = 0;
        for (AuctionItem item : items) {
            VBox card = buildCard(item);
            GridPane.setColumnIndex(card, col);
            GridPane.setRowIndex(card, row);
            cardsGrid.getChildren().add(card);
            if (++col == 3) { col = 0; row++; }
        }
    }

    private VBox buildCard(AuctionItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("al-card");
        card.setPrefWidth(340);

        // Image area
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(220);
        imagePane.setStyle("-fx-background-color: #1e1c15; -fx-background-radius: 12 12 0 0;");

        Label emoji = new Label(emojiFor(item.getCategory()));
        emoji.setStyle("-fx-font-size: 80px;");
        imagePane.getChildren().add(emoji);

        // Badge
        String displayStatus = item.getDisplayStatus();
        String badgeText, badgeStyle;
        if ("CLOSED".equals(displayStatus) || item.isClosed()) {
            badgeText = "● CLOSED";        badgeStyle = "al-badge-upcoming";
        } else if ("ENDING_SOON".equals(displayStatus)) {
            badgeText = "⏰ ENDING SOON"; badgeStyle = "al-badge-ending";
        } else if (item.isPending()) {
            badgeText = "🕐 UPCOMING";   badgeStyle = "al-badge-upcoming";
        } else {
            badgeText = "● LIVE";        badgeStyle = "al-badge-live";
        }
        Label badge = new Label(badgeText);
        badge.setId("badge_" + item.getAuctionId());
        badge.getStyleClass().add(badgeStyle);
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(12, 0, 0, 12));
        imagePane.getChildren().add(badge);

        // Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(16));

        Label title = new Label(item.getItemName());
        title.getStyleClass().add("al-card-title");
        title.setWrapText(true);

        Label subtitle = new Label(item.getDescription());
        subtitle.getStyleClass().add("al-card-subtitle");
        subtitle.setWrapText(true);

        // Bid row
        HBox bidRow = new HBox(8);
        bidRow.setAlignment(Pos.CENTER_LEFT);

        VBox bidInfo = new VBox();
        Label bidLabel = new Label("CURRENT BID");
        bidLabel.getStyleClass().add("al-bid-label");
        Label bidAmount = new Label(String.format("$%,.0f", item.getCurrentPrice()));
        bidAmount.getStyleClass().add("al-bid-amount");
        bidInfo.getChildren().addAll(bidLabel, bidAmount);

        VBox timerInfo = new VBox();
        timerInfo.setAlignment(Pos.CENTER_RIGHT);
        Label timerLabel = new Label("ENDS IN");
        timerLabel.getStyleClass().add("al-timer-label");
        Label timerValue = new Label(formatTime(item.secondsLeft()));
        timerValue.setId("timer_" + item.getAuctionId());
        timerValue.getStyleClass().add(item.secondsLeft() < 900 ? "al-timer-ending" : "al-timer-value");
        timerInfo.getChildren().addAll(timerLabel, timerValue);

        HBox.setHgrow(bidInfo, Priority.ALWAYS);
        bidRow.getChildren().addAll(bidInfo, timerInfo);

        Button bidButton = new Button("PLACE BID");
        bidButton.getStyleClass().add("al-btn-bid");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setOnAction(e -> NavigationUtils.navigateToAuctionDetail(item));

        body.getChildren().addAll(title, subtitle, bidRow, bidButton);
        card.getChildren().addAll(imagePane, body);

        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Button))
                NavigationUtils.navigateToAuctionDetail(item);
        });

        return card;
    }

    // ── Countdown timer ───────────────────────────────────────
    private void startCountdownTimer() {
        clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> {
                    boolean anyEnded = false;
                    for (AuctionItem item : viewModel.getFilteredItems()) {
                        Label lbl = (Label) cardsGrid.lookup("#timer_" + item.getAuctionId());
                        if (lbl != null) {
                            int remaining = item.secondsLeft();
                            lbl.setText(formatTime(remaining));
                            if (remaining < 900)
                                lbl.getStyleClass().setAll("label", "al-timer-ending");
                            
                            // Dynamically update the badge in real-time
                            Label badgeLbl = (Label) cardsGrid.lookup("#badge_" + item.getAuctionId());
                            if (badgeLbl != null) {
                                String displayStatus = item.getDisplayStatus();
                                String badgeText, badgeStyle;
                                if ("CLOSED".equals(displayStatus) || item.isClosed()) {
                                    badgeText = "● CLOSED";        badgeStyle = "al-badge-upcoming";
                                } else if ("ENDING_SOON".equals(displayStatus)) {
                                    badgeText = "⏰ ENDING SOON"; badgeStyle = "al-badge-ending";
                                } else if (item.isPending()) {
                                    badgeText = "🕐 UPCOMING";   badgeStyle = "al-badge-upcoming";
                                } else {
                                    badgeText = "● LIVE";        badgeStyle = "al-badge-live";
                                }
                                if (!badgeLbl.getText().equals(badgeText)) {
                                    badgeLbl.setText(badgeText);
                                    badgeLbl.getStyleClass().setAll("label", badgeStyle);
                                }
                            }

                            // Phát hiện phiên kết thúc và đang trạng thái RUNNING
                            if (remaining <= 0 && item.isRunning()) {
                                anyEnded = true;

                                // Let the server handle closing and payment settlement.
                                // We just refresh user balance from server to get updated winning/lost balance.
                                UserSession.getInstance().refreshUserBalanceFromServer();

                                // Đóng phiên để tránh xử lý lặp lại
                                item.setStatus("CLOSED");
                            }
                        }
                    }
                    if (anyEnded) {
                        // Ẩn sản phẩm đã kết thúc khỏi màn hình Live Auctions
                        refreshCards();
                    }
                    
                    listRefreshCounter++;
                    if (listRefreshCounter >= 3) {
                        listRefreshCounter = 0;
                        new Thread(() -> {
                            viewModel.loadData();
                            UserSession.getInstance().refreshUserBalanceFromServer();
                            Platform.runLater(AuctionListController.this::refreshCards);
                        }).start();
                    }
                });
            }
        }, 1000, 1000);
    }

    // ── Cleanup ───────────────────────────────────────────────
    private void cleanup() {
        if (clockTimer != null) clockTimer.cancel();
        if (balanceListener != null)
            UserSession.getInstance().removeBalanceListener(balanceListener);
    }

    // ── Utilities ─────────────────────────────────────────────
    private String emojiFor(String category) {
        if (category == null) return "⭐";
        return switch (category.toLowerCase()) {
            case "vehicles", "vehicle"            -> "🏎️";
            case "watches", "watch"               -> "⌚";
            case "art", "fine art"                -> "🖼️";
            case "jewelry", "jewellery"           -> "💎";
            case "electronics"                    -> "💻";
            default                               -> "⭐";
        };
    }

    private String formatTime(int seconds) {
        if (seconds <= 0) return "00:00:00";
        return String.format("%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}