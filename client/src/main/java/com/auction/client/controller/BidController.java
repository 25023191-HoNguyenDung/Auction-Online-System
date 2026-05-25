package com.auction.client.controller;

import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

import com.auction.client.model.AuctionItem;
import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;
import com.auction.client.network.ClientMessageSender;
import com.auction.client.network.ServerEventListener;
import com.auction.common.protocol.*;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class BidController {

    @FXML private Button btnBack;

    @FXML private Label itemEmojiLabel;
    @FXML private Label badgeLabel;
    @FXML private Label itemTitleLabel;
    @FXML private Label itemSubtitleLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label timeRemainingLabel;
    @FXML private Label totalBidsLabel;
    @FXML private Label sellerLabel;

    @FXML private Label     minBidLabel;
    @FXML private Label     balanceLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button    quickBid1;
    @FXML private Button    quickBid2;
    @FXML private Button    quickBid3;
    @FXML private Button    quickBid4;
    @FXML private Label     messageLabel;
    @FXML private Button    confirmBidButton;

    // ── AutoBid FXML Injections ──
    @FXML private Button btnManualMode;
    @FXML private Button btnAutoMode;
    @FXML private javafx.scene.layout.VBox manualBidSection;
    @FXML private javafx.scene.layout.VBox autoBidSection;
    @FXML private Label balanceLabelAuto;
    @FXML private TextField autoMaxBidField;
    @FXML private TextField autoIncrementField;
    @FXML private Label autoBidStatusLabel;
    @FXML private Button btnActivateAutoBid;
    @FXML private Button btnCancelAutoBid;

    @FXML private ListView<String> bidHistoryList;
    @FXML private Label            bidCountLabel;
    @FXML private LineChart<String, Number> priceChart;

    private AuctionItem currentItem;
    private double      currentBid   = 0;
    private double      prevWinBid   = 0;   // last accepted bid so we can refund on outbid
    private Timer       countdownTimer;

    // Listener refs — held so we can unregister on cleanup
    private Consumer<Double>                          balanceListener;
    private Consumer<UserSession.Transaction>         transactionListener;

    // ── Lifecycle ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (btnBack != null) {
            btnBack.setOnAction(e -> {
                cleanup();
                if (currentItem != null) {
                    NavigationUtils.navigateToAuctionDetail(currentItem);
                } else {
                    NavigationUtils.navigateTo(
                        "/com/auction/client/view/AuctionList.fxml", "Live Auctions");
                }
            });
        }
        setupBidHistoryList();

        // Keep balance label live whenever another screen changes the balance
        balanceListener = newBal -> Platform.runLater(this::refreshBalanceLabel);
        UserSession.getInstance().addBalanceListener(balanceListener);

        // Keep bid history in sync with new transactions from this session
        transactionListener = t -> Platform.runLater(() -> prependTransaction(t));
        UserSession.getInstance().addTransactionListener(transactionListener);

        if (priceChart != null) {
            priceChart.setCursor(javafx.scene.Cursor.HAND);
            priceChart.setOnMouseClicked(event -> showEnlargedChart());
        }
        updateModeTabStyles(true);
    }

    // ── Item injection (called by NavigationUtils) ────────────
    public void setAuctionItem(AuctionItem item) {
        this.currentItem = item;
        this.currentBid  = item.getCurrentPrice();
        this.prevWinBid  = 0;

        itemEmojiLabel.setText(emojiFor(item.getCategory()));
        itemTitleLabel.setText(item.getItemName());
        itemSubtitleLabel.setText(item.getDescription() != null ? item.getDescription() : "");
        currentBidLabel.setText(fmt(currentBid));
        timeRemainingLabel.setText(formatTime(item.secondsLeft()));
        totalBidsLabel.setText(String.valueOf(item.getTotalBids()));
        sellerLabel.setText(item.getSellerName() != null ? item.getSellerName() : "—");

        if (item.isClosed()) {
            badgeLabel.setText("● CLOSED");
            badgeLabel.getStyleClass().setAll("al-badge-upcoming");
            if (confirmBidButton != null) confirmBidButton.setDisable(true);
            if (quickBid1 != null) quickBid1.setDisable(true);
            if (quickBid2 != null) quickBid2.setDisable(true);
            if (quickBid3 != null) quickBid3.setDisable(true);
            if (quickBid4 != null) quickBid4.setDisable(true);
            if (bidAmountField != null) bidAmountField.setDisable(true);
            showError("This auction has closed.");
        } else if (item.isPending()) {
            badgeLabel.setText("🕐 UPCOMING");
            badgeLabel.getStyleClass().setAll("al-badge-upcoming");
            if (confirmBidButton != null) confirmBidButton.setDisable(true);
            if (quickBid1 != null) quickBid1.setDisable(true);
            if (quickBid2 != null) quickBid2.setDisable(true);
            if (quickBid3 != null) quickBid3.setDisable(true);
            if (quickBid4 != null) quickBid4.setDisable(true);
            if (bidAmountField != null) bidAmountField.setDisable(true);
            showError("This auction is upcoming and has not started yet.");
        } else if (item.isEndingSoon()) {
            badgeLabel.setText("⏰ ENDING SOON");
            badgeLabel.getStyleClass().setAll("al-badge-ending");
        } else {
            badgeLabel.setText("● LIVE");
            badgeLabel.getStyleClass().setAll("al-badge-live");
        }

        double minBid = currentBid + 1;
        minBidLabel.setText(fmt(minBid));
        bidAmountField.setPromptText(fmt(minBid));

        refreshBalanceLabel();
        seedMockHistory(item);
        updateChartData(item.getBidHistory(), item.getStartingPrice());
        startCountdownTimer();

        // Subscribe to real-time updates for this auction on the server
        try {
            ClientMessageSender sender = new ClientMessageSender();
            sender.sendSubscribe(item.getAuctionId());
            System.out.println(">>> Subscribed to auction " + item.getAuctionId() + " for real-time bid updates.");
        } catch (Exception ex) {
            System.err.println(">>> Failed to send SUBSCRIBE_REQ: " + ex.getMessage());
        }

        // Listen for real-time bid updates from the server
        ServerEventListener.getActiveInstance().setOnBidUpdated(payload -> {
            if (payload == null || currentItem == null || payload.getAuctionId() != currentItem.getAuctionId()) return;

            Platform.runLater(() -> {
                double newPrice = payload.getNewHighestBid().doubleValue();
                
                // Synchronize Anti-Sniping locally on client
                int secondsLeft = currentItem.secondsLeft();
                if (secondsLeft > 0 && secondsLeft <= 30) {
                    currentItem.setEndTime(currentItem.getEndTime().plusSeconds(60));
                    System.out.println("[AntiSnipe] Extended local end time by 60 seconds. New end time: " + currentItem.getEndTime());
                }

                // Update local attributes of the item
                currentItem.setCurrentPrice(newPrice);
                currentItem.setTotalBids(currentItem.getTotalBids() + 1);

                currentBid = newPrice;
                currentBidLabel.setText(fmt(currentBid));
                minBidLabel.setText(fmt(currentBid + 1));
                bidAmountField.setPromptText(fmt(currentBid + 1));
                totalBidsLabel.setText(String.valueOf(currentItem.getTotalBids()));

                // Fetch bidder name (resolve from cache or fallback)
                String bidderName = UserSession.getInstance().getUsernameById(payload.getLeaderBidderId());
                String bidTimeString = java.time.LocalDateTime.ofInstant(payload.getBidTime(), java.time.ZoneId.systemDefault()).toString();

                String entry = bidderName + "  →  " + fmt(newPrice) + "  →  " + bidTimeString;
                if (currentItem.getBidHistory() == null) {
                    currentItem.setBidHistory(new java.util.ArrayList<>());
                }
                
                // Avoid duplicates by comparing numeric amount
                boolean exists = false;
                for (String hist : currentItem.getBidHistory()) {
                    String[] parts = hist.split("  →  ");
                    if (parts.length > 1) {
                        try {
                            String amtStr = parts[1].replace("$", "").replace(",", "").trim();
                            double amount = Double.parseDouble(amtStr);
                            if (Math.abs(amount - newPrice) < 0.01) {
                                exists = true;
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (!exists) {
                    currentItem.getBidHistory().add(0, entry);
                }

                // Refresh history list and price curve
                seedMockHistory(currentItem);
                updateChartData(currentItem.getBidHistory(), currentItem.getStartingPrice());
                
                // Re-fetch auto bid status to keep UI in sync
                fetchAutoBidStatus();

                System.out.println("⚡ Real-time bid update received! New highest bid: " + fmt(newPrice) + " by " + bidderName);
            });
        });
        fetchAutoBidStatus();
    }

    // ── Balance label ─────────────────────────────────────────
    private void refreshBalanceLabel() {
        double bal = UserSession.getInstance().getBalance();
        if (balanceLabel != null) {
            balanceLabel.setText(fmt(bal));
            if (bal >= 10_000) {
                balanceLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else if (bal >= 1_000) {
                balanceLabel.setStyle("-fx-text-fill: #f0b429; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else {
                balanceLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-font-weight: bold;");
            }
        }
        if (balanceLabelAuto != null) {
            balanceLabelAuto.setText(fmt(bal));
            if (bal >= 10_000) {
                balanceLabelAuto.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else if (bal >= 1_000) {
                balanceLabelAuto.setStyle("-fx-text-fill: #f0b429; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else {
                balanceLabelAuto.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-font-weight: bold;");
            }
        }
    }

    // ── History navigation ────────────────────────────────────
    @FXML
    private void handleNavHistory(MouseEvent event) {
        cleanup();
        NavigationUtils.navigateToBidHistory();
    }

    // ── Countdown timer ───────────────────────────────────────
    private void startCountdownTimer() {
        stopTimer();
        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (currentItem == null || timeRemainingLabel == null) return;

                    int seconds = currentItem.secondsLeft();
                    timeRemainingLabel.setText(formatTime(seconds));

                    if (seconds <= 0) {
                        stopTimer();
                        if (confirmBidButton != null) confirmBidButton.setDisable(true);
                        if (badgeLabel != null) {
                            badgeLabel.setText("● CLOSED");
                            badgeLabel.getStyleClass().setAll("label", "al-badge-upcoming");
                        }
                        showError("Auction has ended.");
                    } else if (seconds <= 300) {
                        timeRemainingLabel.getStyleClass().setAll("label", "ad-timer-ending");
                        if (badgeLabel != null && !"⏰ ENDING SOON".equals(badgeLabel.getText())) {
                            badgeLabel.setText("⏰ ENDING SOON");
                            badgeLabel.getStyleClass().setAll("label", "al-badge-ending");
                        }
                    } else {
                        timeRemainingLabel.getStyleClass().setAll("label", "ad-timer-value");
                        if (badgeLabel != null && !"● LIVE".equals(badgeLabel.getText())) {
                            badgeLabel.setText("● LIVE");
                            badgeLabel.getStyleClass().setAll("label", "al-badge-live");
                        }
                    }
                });
            }
        }, 1000, 1000);
    }

    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    // ── Quick bid ─────────────────────────────────────────────
    @FXML
    private void handleQuickBid(javafx.event.ActionEvent e) {
        Button src = (Button) e.getSource();
        String raw = src.getText().replaceAll("[^0-9.]", "");
        try {
            double increment = Double.parseDouble(raw);
            bidAmountField.setText(String.format("%.0f", currentBid + increment));
            hideMessage();
        } catch (NumberFormatException ex) {
            showError("Invalid quick bid amount.");
        }
    }

    // ── Confirm bid ───────────────────────────────────────────
    @FXML
    private void handleConfirmBid() {
        hideMessage();

        if (isCurrentUserLeading()) {
            showError("Bạn đang dẫn đầu phiên đấu giá. Không thể tự đặt giá cao hơn chính mình!");
            return;
        }

        String raw = bidAmountField.getText().trim().replace(",", "");
        if (raw.isEmpty()) { showError("Please enter a bid amount."); return; }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            showError("Invalid amount — numbers only.");
            return;
        }

        if (amount <= currentBid) {
            showError("Your bid must be higher than the current bid of " + fmt(currentBid) + ".");
            return;
        }

        double balance = UserSession.getInstance().getBalance();
        if (amount > balance) {
            showError(String.format(
                "Insufficient balance. Your balance is %s.", fmt(balance)));
            return;
        }

        // Disable button while sending to avoid double submission
        if (confirmBidButton != null) confirmBidButton.setDisable(true);
        showSuccess("Submitting bid to server...");

        // Gửi Place Bid Req lên Server
        new Thread(() -> {
            try {
                CompletableFuture<MessageEnvelope> responseFuture = new CompletableFuture<>();
                ClientMessageSender sender = new ClientMessageSender();
                
                long bidderId = UserSession.getInstance().getCurrentUser().getId();
                long auctionId = currentItem.getAuctionId();
                java.math.BigDecimal bidAmount = new java.math.BigDecimal(amount);
                
                String messageId = sender.sendPlaceBid(auctionId, bidderId, bidAmount);
                ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);
                
                MessageEnvelope resEnvelope = responseFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                
                Platform.runLater(() -> {
                    if (confirmBidButton != null) confirmBidButton.setDisable(false);
                    
                    if (resEnvelope.getType() == MessageType.ERROR_RES) {
                        try {
                            ErrorPayload err = new ProtocolMapper().parsePayload(resEnvelope, ErrorPayload.class);
                            showError("Failed: " + err.getMessage());
                        } catch (Exception ex) {
                            showError("Bid rejected by server.");
                        }
                    } else {
                        // Thành công! Tiến hành cập nhật local và ví
                        boolean ok = UserSession.getInstance().placeBid(currentItem.getItemName(), amount);
                        if (!ok) {
                            showError("Bid could not be processed locally.");
                            return;
                        }
                        
                        // Cập nhật thuộc tính của item để mang đi các màn hình khác
                        currentItem.setCurrentPrice(amount);
                        currentItem.setTotalBids(currentItem.getTotalBids() + 1);

                        prevWinBid = amount;
                        currentBid = amount;
                        currentBidLabel.setText(fmt(currentBid));
                        minBidLabel.setText(fmt(currentBid + 1));
                        bidAmountField.clear();

                        String bidder = UserSession.getInstance().isLoggedIn()
                            ? UserSession.getInstance().getCurrentUser().getUsername()
                            : "You";

                        String timestamp = java.time.LocalDateTime.now().toString();
                        String entry = bidder + "  →  " + fmt(amount) + "  →  " + timestamp;
                        if (currentItem.getBidHistory() == null) {
                            currentItem.setBidHistory(new java.util.ArrayList<>());
                        }
                        currentItem.getBidHistory().add(0, entry);

                        // Reload list and update chart
                        seedMockHistory(currentItem);
                        updateChartData(currentItem.getBidHistory(), currentItem.getStartingPrice());

                        showSuccess("Bid of " + fmt(amount) + " placed successfully!");
                        System.out.println("✅ Bid placed on server: " + fmt(amount) + " on " + currentItem.getItemName());
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (confirmBidButton != null) confirmBidButton.setDisable(false);
                    showError("Network timeout. Please try again.");
                });
            }
        }).start();
    }

    // ── Prepend a new transaction to the bid history list ─────
    private void prependTransaction(UserSession.Transaction t) {
        if (t.kind != UserSession.Transaction.Kind.BID) return;
        if (currentItem == null || !t.itemName.equals(currentItem.getItemName())) return;
        
        // Check if there is already an entry in bidHistoryList with the same amount
        for (String item : bidHistoryList.getItems()) {
            String[] parts = item.split("  →  ");
            if (parts.length > 1) {
                try {
                    String amtStr = parts[1].replace("$", "").replace(",", "").trim();
                    double amount = Double.parseDouble(amtStr);
                    if (Math.abs(amount - t.amount) < 0.01) {
                        return; // Already in the list, avoid duplicate!
                    }
                } catch (Exception ignored) {}
            }
        }

        String username = UserSession.getInstance().isLoggedIn()
                ? UserSession.getInstance().getCurrentUser().getUsername() : "You";
        String entry = username + "  →  " + fmt(t.amount) + "  →  " + t.time.toString();
        
        // Add to current item bid history if not already present
        if (currentItem.getBidHistory() == null) {
            currentItem.setBidHistory(new java.util.ArrayList<>());
        }
        boolean existsInHistory = false;
        for (String hist : currentItem.getBidHistory()) {
            String[] parts = hist.split("  →  ");
            if (parts.length > 1) {
                try {
                    String amtStr = parts[1].replace("$", "").replace(",", "").trim();
                    double amount = Double.parseDouble(amtStr);
                    if (Math.abs(amount - t.amount) < 0.01) {
                        existsInHistory = true;
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }
        if (!existsInHistory) {
            currentItem.getBidHistory().add(0, entry);
        }

        bidHistoryList.getItems().add(0, entry);
        bidCountLabel.setText(bidHistoryList.getItems().size() + " bids");
        
        // Update chart data as well!
        updateChartData(currentItem.getBidHistory(), currentItem.getStartingPrice());
    }

    // ── Bid history list ──────────────────────────────────────
    private void setupBidHistoryList() {
        bidHistoryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }

                String[] parts = item.split("  →  ");
                HBox row = new HBox();
                row.setStyle("-fx-padding: 6 0;");

                Label bidder = new Label(parts.length > 0 ? parts[0] : "");
                bidder.setStyle("-fx-text-fill: #d4cdb8; -fx-font-size: 12px; -fx-font-family: 'Arial';");

                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                Label amountLbl = new Label(parts.length > 1 ? parts[1] : "");
                amountLbl.setStyle("-fx-text-fill: #f0b429; -fx-font-size: 13px;" +
                                   " -fx-font-weight: bold; -fx-font-family: 'Arial';");

                if (getIndex() == 0) {
                    bidder.setStyle(bidder.getStyle() + " -fx-text-fill: #f5f0e6;");
                    amountLbl.setStyle(amountLbl.getStyle() + " -fx-text-fill: #4ade80;");
                }

                row.getChildren().addAll(bidder, spacer, amountLbl);
                setGraphic(row);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            }
        });
    }

    private void seedMockHistory(AuctionItem item) {
        bidHistoryList.getItems().clear();
        if (item.getBidHistory() != null) {
            for (String b : item.getBidHistory()) {
                bidHistoryList.getItems().add(b);
            }
        }
        bidCountLabel.setText(bidHistoryList.getItems().size() + " bids");
    }

    private void updateChartData(List<String> history, double startingPrice) {
        if (priceChart == null) return;
        priceChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Price Trend");

        // Start with the starting price as the base point
        series.getData().add(new XYChart.Data<>("Start", startingPrice));

        if (history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= 0; i--) {
                String entry = history.get(i);
                String[] parts = entry.split("  →  ");
                if (parts.length > 1) {
                    try {
                        String amtStr = parts[1].replace("$", "").replace(",", "").trim();
                        double amount = Double.parseDouble(amtStr);

                        String timeLabel = "Bid " + (history.size() - i);
                        if (parts.length > 2) {
                            try {
                                java.time.LocalDateTime bidTime = java.time.LocalDateTime.parse(parts[2].trim());
                                timeLabel = bidTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                            } catch (Exception ex) {
                                // Fallback
                            }
                        }
                        series.getData().add(new XYChart.Data<>(timeLabel, amount));
                    } catch (Exception e) {
                        System.err.println("Error parsing bid history entry for chart: " + e.getMessage());
                    }
                }
            }
        }

        priceChart.getData().add(series);
        installChartTooltips(series);
    }

    private void installChartTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    double val = data.getYValue().doubleValue();
                    javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(String.format("$%,.0f", val));
                    tooltip.setStyle("-fx-background-color: #1a1813; -fx-text-fill: #f0b429; -fx-font-weight: bold; -fx-border-color: #f0b429; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-padding: 6px 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 8, 0, 0, 4);");
                    tooltip.setShowDelay(javafx.util.Duration.millis(50));
                    javafx.scene.control.Tooltip.install(newNode, tooltip);
                }
            });
            javafx.scene.Node node = data.getNode();
            if (node != null) {
                double val = data.getYValue().doubleValue();
                javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(String.format("$%,.0f", val));
                tooltip.setStyle("-fx-background-color: #1a1813; -fx-text-fill: #f0b429; -fx-font-weight: bold; -fx-border-color: #f0b429; -fx-border-width: 1; -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 12px; -fx-padding: 6px 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 8, 0, 0, 4);");
                tooltip.setShowDelay(javafx.util.Duration.millis(50));
                javafx.scene.control.Tooltip.install(node, tooltip);
            }
        }
    }


    private void showEnlargedChart() {
        if (currentItem == null) return;

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Enlarged Price Curve - " + currentItem.getItemName());
        
        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        xAxis.setLabel("Time");
        javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
        yAxis.setLabel("Price ($)");
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);

        javafx.scene.chart.LineChart<String, Number> largeChart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        largeChart.setCreateSymbols(true);
        largeChart.setLegendVisible(false);
        largeChart.getStyleClass().add("ad-price-chart");
        largeChart.setStyle("-fx-background-color: #161410; -fx-padding: 20;");

        // Clone current series data
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Price Trend");
        
        // Add start price
        series.getData().add(new javafx.scene.chart.XYChart.Data<>("Start", currentItem.getStartingPrice()));

        List<String> history = currentItem.getBidHistory();
        if (history != null) {
            for (int i = history.size() - 1; i >= 0; i--) {
                String entry = history.get(i);
                String[] parts = entry.split("  →  ");
                if (parts.length > 1) {
                    try {
                        double amount = Double.parseDouble(parts[1].replace("$", "").replace(",", "").trim());
                        String timeLabel = "Bid " + (history.size() - i);
                        if (parts.length > 2) {
                            try {
                                java.time.LocalDateTime bidTime = java.time.LocalDateTime.parse(parts[2].trim());
                                timeLabel = bidTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                            } catch (Exception ignored) {}
                        }
                        series.getData().add(new javafx.scene.chart.XYChart.Data<>(timeLabel, amount));
                    } catch (Exception ignored) {}
                }
            }
        }
        largeChart.getData().add(series);
        installChartTooltips(series);

        javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox(15, largeChart);
        layout.setStyle("-fx-background-color: #0d0c08; -fx-padding: 25;");
        javafx.scene.layout.VBox.setVgrow(largeChart, javafx.scene.layout.Priority.ALWAYS);

        // Add a beautiful close button
        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("Close View");
        closeBtn.getStyleClass().add("ad-btn-back");
        closeBtn.setOnAction(e -> dialog.close());
        layout.getChildren().add(closeBtn);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 800, 550);
        java.net.URL cssUrl = getClass().getResource("/com/auction/client/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        
        dialog.setScene(scene);
        dialog.showAndWait();
    }


    // ── Cleanup ───────────────────────────────────────────────
    private void cleanup() {
        stopTimer();
        if (balanceListener != null)
            UserSession.getInstance().removeBalanceListener(balanceListener);
        if (transactionListener != null)
            UserSession.getInstance().removeTransactionListener(transactionListener);

        if (currentItem != null) {
            try {
                ClientMessageSender sender = new ClientMessageSender();
                sender.sendUnsubscribe(currentItem.getAuctionId());
                System.out.println(">>> Unsubscribed from auction " + currentItem.getAuctionId());
            } catch (Exception ex) {
                System.err.println(">>> Failed to send UNSUBSCRIBE_REQ: " + ex.getMessage());
            }
        }
        // Clear real-time event listener references to avoid memory leaks
        ServerEventListener.getActiveInstance().setOnBidUpdated(null);
    }

    // ── Utilities ─────────────────────────────────────────────
    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        messageLabel.setVisible(true);
    }

    private void showSuccess(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");
        messageLabel.setVisible(true);
    }

    private void hideMessage() { messageLabel.setVisible(false); }

    private String fmt(double value) { return String.format("$%,.0f", value); }

    private String formatTime(int seconds) {
        if (seconds <= 0) return "00:00:00";
        return String.format("%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

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

    // ── AutoBid Actions & Logic ──
    @FXML
    private void handleManualModeClick() {
        hideMessage();
        updateModeTabStyles(true);
        manualBidSection.setVisible(true);
        manualBidSection.setManaged(true);
        autoBidSection.setVisible(false);
        autoBidSection.setManaged(false);
    }

    @FXML
    private void handleAutoModeClick() {
        hideMessage();
        updateModeTabStyles(false);
        manualBidSection.setVisible(false);
        manualBidSection.setManaged(false);
        autoBidSection.setVisible(true);
        autoBidSection.setManaged(true);
    }

    private void updateModeTabStyles(boolean manual) {
        if (btnManualMode != null && btnAutoMode != null) {
            if (manual) {
                btnManualMode.setStyle("-fx-background-color: #f0b429; -fx-text-fill: #0d0c08; -fx-font-weight: bold;");
                btnAutoMode.setStyle("-fx-background-color: transparent; -fx-text-fill: #8a8272; -fx-font-weight: normal;");
            } else {
                btnManualMode.setStyle("-fx-background-color: transparent; -fx-text-fill: #8a8272; -fx-font-weight: normal;");
                btnAutoMode.setStyle("-fx-background-color: #f0b429; -fx-text-fill: #0d0c08; -fx-font-weight: bold;");
            }
        }
    }

    @FXML
    private void handleActivateAutoBid() {
        hideMessage();

        if (bidHistoryList == null || bidHistoryList.getItems().isEmpty()) {
            showError("Chưa có lượt đặt giá nào. Hãy đặt giá khởi điểm thủ công trước khi kích hoạt AutoBid.");
            return;
        }

        if (isCurrentUserLeading()) {
            showError("Bạn đang dẫn đầu phiên đấu giá. Chỉ có thể kích hoạt AutoBid sau khi người khác đặt giá cao hơn.");
            return;
        }

        String rawMax = autoMaxBidField.getText().trim().replace(",", "");
        String rawInc = autoIncrementField.getText().trim().replace(",", "");

        if (rawMax.isEmpty() || rawInc.isEmpty()) {
            showError("Please enter both Max Bid and Increment values.");
            return;
        }

        double maxBid;
        double increment;
        try {
            maxBid = Double.parseDouble(rawMax);
            increment = Double.parseDouble(rawInc);
        } catch (NumberFormatException ex) {
            showError("Invalid input - numeric values only.");
            return;
        }

        if (maxBid <= 0 || increment <= 0) {
            showError("Both values must be positive.");
            return;
        }

        if (maxBid <= currentBid + increment) {
            showError("Max Bid must be higher than current bid + increment ($" + String.format("%,.0f", currentBid + increment) + ").");
            return;
        }

        double balance = UserSession.getInstance().getBalance();
        if (maxBid > balance) {
            showError(String.format("Insufficient balance. Your balance is %s.", fmt(balance)));
            return;
        }

        btnActivateAutoBid.setDisable(true);
        showSuccess("Activating AutoBid...");

        new Thread(() -> {
            try {
                CompletableFuture<MessageEnvelope> responseFuture = new CompletableFuture<>();
                ClientMessageSender sender = new ClientMessageSender();
                long userId = UserSession.getInstance().getCurrentUser().getId();
                long auctionId = currentItem.getAuctionId();

                String messageId = sender.sendRegisterAutoBid(userId, auctionId, new java.math.BigDecimal(maxBid), new java.math.BigDecimal(increment));
                ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);

                MessageEnvelope resEnvelope = responseFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                Platform.runLater(() -> {
                    btnActivateAutoBid.setDisable(false);
                    if (resEnvelope.getType() == MessageType.ERROR_RES) {
                        try {
                            ErrorPayload err = new ProtocolMapper().parsePayload(resEnvelope, ErrorPayload.class);
                            showError("Failed: " + err.getMessage());
                        } catch (Exception ex) {
                            showError("AutoBid registration rejected by server.");
                        }
                    } else {
                        try {
                            RegisterAutoBidResPayload res = new ProtocolMapper().parsePayload(resEnvelope, RegisterAutoBidResPayload.class);
                            if (res.isSuccess()) {
                                showSuccess("AutoBid activated successfully!");
                                fetchAutoBidStatus();
                            } else {
                                showError("Failed: " + res.getMessage());
                            }
                        } catch (Exception ex) {
                            showError("Failed to parse response.");
                        }
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    btnActivateAutoBid.setDisable(false);
                    showError("Timeout connecting to server.");
                });
            }
        }).start();
    }

    @FXML
    private void handleCancelAutoBid() {
        hideMessage();
        btnCancelAutoBid.setDisable(true);
        showSuccess("Canceling AutoBid...");

        new Thread(() -> {
            try {
                CompletableFuture<MessageEnvelope> responseFuture = new CompletableFuture<>();
                ClientMessageSender sender = new ClientMessageSender();
                long userId = UserSession.getInstance().getCurrentUser().getId();
                long auctionId = currentItem.getAuctionId();

                String messageId = sender.sendCancelAutoBid(userId, auctionId);
                ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);

                MessageEnvelope resEnvelope = responseFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                Platform.runLater(() -> {
                    btnCancelAutoBid.setDisable(false);
                    if (resEnvelope.getType() == MessageType.ERROR_RES) {
                        try {
                            ErrorPayload err = new ProtocolMapper().parsePayload(resEnvelope, ErrorPayload.class);
                            showError("Failed: " + err.getMessage());
                        } catch (Exception ex) {
                            showError("AutoBid cancellation rejected by server.");
                        }
                    } else {
                        try {
                            CancelAutoBidResPayload res = new ProtocolMapper().parsePayload(resEnvelope, CancelAutoBidResPayload.class);
                            if (res.isSuccess()) {
                                showSuccess("AutoBid canceled successfully!");
                                fetchAutoBidStatus();
                            } else {
                                showError("Failed: " + res.getMessage());
                            }
                        } catch (Exception ex) {
                            showError("Failed to parse response.");
                        }
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    btnCancelAutoBid.setDisable(false);
                    showError("Timeout connecting to server.");
                });
            }
        }).start();
    }

    private void fetchAutoBidStatus() {
        if (currentItem == null || !UserSession.getInstance().isLoggedIn()) return;

        new Thread(() -> {
            try {
                CompletableFuture<MessageEnvelope> responseFuture = new CompletableFuture<>();
                ClientMessageSender sender = new ClientMessageSender();
                long userId = UserSession.getInstance().getCurrentUser().getId();
                long auctionId = currentItem.getAuctionId();

                String messageId = sender.sendGetAutoBid(userId, auctionId);
                ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);

                MessageEnvelope resEnvelope = responseFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
                Platform.runLater(() -> {
                    if (resEnvelope.getType() == MessageType.GET_AUTOBID_RES) {
                        try {
                            GetAutoBidResPayload payload = new ProtocolMapper().parsePayload(resEnvelope, GetAutoBidResPayload.class);
                            if (payload.isActive()) {
                                // Prefill fields
                                autoMaxBidField.setText(String.format("%.0f", payload.getMaxBid().doubleValue()));
                                autoIncrementField.setText(String.format("%.0f", payload.getIncrement().doubleValue()));
                                
                                // Disable fields
                                autoMaxBidField.setDisable(true);
                                autoIncrementField.setDisable(true);
                                
                                // Adjust button visibility
                                btnActivateAutoBid.setVisible(false);
                                btnActivateAutoBid.setManaged(false);
                                btnCancelAutoBid.setVisible(true);
                                btnCancelAutoBid.setManaged(true);
                                
                                // Update status label
                                autoBidStatusLabel.setText("🤖 AutoBid is active up to $" + String.format("%,.0f", payload.getMaxBid().doubleValue()) + " (increment: $" + String.format("%,.0f", payload.getIncrement().doubleValue()) + ")");
                            } else {
                                // Enable fields
                                autoMaxBidField.clear();
                                autoIncrementField.clear();
                                autoMaxBidField.setDisable(false);
                                autoIncrementField.setDisable(false);
                                
                                // Adjust button visibility
                                btnActivateAutoBid.setVisible(true);
                                btnActivateAutoBid.setManaged(true);
                                btnCancelAutoBid.setVisible(false);
                                btnCancelAutoBid.setManaged(false);
                                
                                // Update status label
                                autoBidStatusLabel.setText("");
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            } catch (Exception ex) {
                System.err.println("Failed to fetch AutoBid status: " + ex.getMessage());
            }
        }).start();
    }

    private boolean isCurrentUserLeading() {
        if (bidHistoryList == null || bidHistoryList.getItems().isEmpty()) {
            return false;
        }
        try {
            String latestBid = bidHistoryList.getItems().get(0);
            String[] parts = latestBid.split("  →  ");
            if (parts.length > 0) {
                String leader = parts[0].trim();
                if (UserSession.getInstance().isLoggedIn()) {
                    String currentUser = UserSession.getInstance().getCurrentUser().getUsername();
                    return currentUser.equalsIgnoreCase(leader) || "You".equalsIgnoreCase(leader);
                } else {
                    return "You".equalsIgnoreCase(leader);
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
