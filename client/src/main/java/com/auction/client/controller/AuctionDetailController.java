package com.auction.client.controller;

import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.function.Consumer;

import com.auction.client.model.AuctionItem;
import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;
import com.auction.client.viewmodel.AuctionDetailViewModel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class AuctionDetailController {

    @FXML private TextField searchField;
    @FXML private Label     currentBidLabel;
    @FXML private Label     timeRemainingLabel;
    @FXML private Label     titleLabel;
    @FXML private Label     subtitleLabel;
    @FXML private Button    placeBidButton;
    @FXML private Button    btnBack;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private TextField bidAmountField;

    /**
     * Optional balance label that can be added to AuctionDetail.fxml.
     * If the label is absent from the FXML it is simply ignored.
     * Add:  <Label fx:id="balanceLabel" .../>  anywhere in the detail panel
     * to surface the live balance here too.
     */
    @FXML private Label balanceLabel;

    @FXML private Label badgeLabel;
    @FXML private Label reserveMetLabel;
    @FXML private Label biddersCountLabel;
    @FXML private Label momentumLabel;

    private final AuctionDetailViewModel viewModel      = new AuctionDetailViewModel();
    private Timer                        countdownTimer;
    private Consumer<Double>             balanceListener;

    // ── Lifecycle ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (btnBack != null) {
            btnBack.setOnAction(e -> {
                cleanup();
                NavigationUtils.navigateTo(
                    "/com/auction/client/view/AuctionList.fxml", "Live Auctions");
            });
        }

        // Subscribe to balance changes so the label stays in sync
        balanceListener = newBal -> Platform.runLater(this::refreshBalanceLabel);
        UserSession.getInstance().addBalanceListener(balanceListener);
        refreshBalanceLabel();

        if (priceChart != null) {
            priceChart.setCursor(javafx.scene.Cursor.HAND);
            priceChart.setOnMouseClicked(event -> showEnlargedChart());
        }
    }

    public void setAuctionItem(AuctionItem item) {
        viewModel.setItem(item);

        if (titleLabel         != null) titleLabel.setText(viewModel.getDisplayTitle());
        if (subtitleLabel      != null) subtitleLabel.setText(viewModel.getDisplaySubtitle());
        if (currentBidLabel    != null) currentBidLabel.setText(viewModel.getDisplayPrice());
        if (timeRemainingLabel != null) timeRemainingLabel.setText(viewModel.getDisplayTimeRemaining());

        refreshBalanceLabel();
        setupChart();
        startCountdownTimer();
        
        // Initial sync of the status badge, bidders participating, and momentum fields
        updateDynamicMetaFields(item);

        // Subscribe to real-time updates for this auction on the server
        try {
            com.auction.client.network.ClientMessageSender sender = new com.auction.client.network.ClientMessageSender();
            sender.sendSubscribe(item.getAuctionId());
            System.out.println(">>> Subscribed to auction " + item.getAuctionId() + " for real-time updates.");
        } catch (Exception ex) {
            System.err.println(">>> Failed to subscribe: " + ex.getMessage());
        }

        // Listen for real-time updates from the server
        com.auction.client.network.ServerEventListener.getActiveInstance().setOnBidUpdated(payload -> {
            if (payload == null || viewModel.getItem() == null || payload.getAuctionId() != viewModel.getItem().getAuctionId()) return;

            Platform.runLater(() -> {
                double newPrice = payload.getNewHighestBid().doubleValue();
                
                // Update local attributes of the item
                viewModel.getItem().setCurrentPrice(newPrice);
                viewModel.getItem().setTotalBids(viewModel.getItem().getTotalBids() + 1);

                if (currentBidLabel != null) currentBidLabel.setText(viewModel.getDisplayPrice());

                // Fetch bidder name
                String bidderName = UserSession.getInstance().getUsernameById(payload.getLeaderBidderId());
                String bidTimeString = java.time.LocalDateTime.ofInstant(payload.getBidTime(), java.time.ZoneId.systemDefault()).toString();

                String entry = bidderName + "  →  " + String.format("$%,.0f", newPrice) + "  →  " + bidTimeString;
                if (viewModel.getItem().getBidHistory() == null) {
                    viewModel.getItem().setBidHistory(new java.util.ArrayList<>());
                }
                
                boolean exists = false;
                for (String hist : viewModel.getItem().getBidHistory()) {
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
                    viewModel.getItem().getBidHistory().add(0, entry);
                }

                // Redraw line chart
                setupChart();

                // Update status badge, bidders participating, and momentum fields dynamically
                updateDynamicMetaFields(viewModel.getItem());

                System.out.println("⚡ Real-time bid update received on detail screen! New highest bid: " + newPrice + " by " + bidderName);
            });
        });
    }

    // ── Balance label ─────────────────────────────────────────
    private void refreshBalanceLabel() {
        if (balanceLabel == null) return;
        double bal = UserSession.getInstance().getBalance();
        balanceLabel.setText(String.format("$%,.0f", bal));
        String color = bal >= 10_000 ? "#4ade80" : bal >= 1_000 ? "#f0b429" : "#ef4444";
        balanceLabel.setStyle("-fx-text-fill:" + color +
                "; -fx-font-size:14px; -fx-font-weight:bold; -fx-font-family:'Arial';");
    }

    // ── Handlers ──────────────────────────────────────────────
    @FXML
    private void handlePlaceBid() {
        AuctionItem item = viewModel.getItem();
        if (item != null) {
            cleanup();
            NavigationUtils.navigateToBidScreen(item);
        }
    }

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
                    AuctionItem item = viewModel.getItem();
                    if (item == null || timeRemainingLabel == null) return;

                    int seconds = item.secondsLeft();
                    timeRemainingLabel.setText(formatTime(seconds));

                    if (seconds < 900) {
                        timeRemainingLabel.getStyleClass().setAll("ad-timer-ending");
                    } else {
                        timeRemainingLabel.getStyleClass().setAll("ad-timer-value");
                    }

                    if (seconds <= 0) {
                        stopTimer();
                    }

                    // Keep dynamic elements updated as time progresses
                    updateDynamicMetaFields(item);
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

    // ── Chart ─────────────────────────────────────────────────
    private void setupChart() {
        if (priceChart == null || viewModel.getItem() == null) return;
        priceChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Price Trend");

        AuctionItem item = viewModel.getItem();
        series.getData().add(new XYChart.Data<>("Start", item.getStartingPrice()));

        List<String> history = item.getBidHistory();
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
        if (viewModel.getItem() == null) return;
        AuctionItem currentItem = viewModel.getItem();

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

        if (viewModel.getItem() != null) {
            try {
                com.auction.client.network.ClientMessageSender sender = new com.auction.client.network.ClientMessageSender();
                sender.sendUnsubscribe(viewModel.getItem().getAuctionId());
                System.out.println(">>> Unsubscribed from auction " + viewModel.getItem().getAuctionId());
            } catch (Exception ex) {
                System.err.println(">>> Failed to unsubscribe: " + ex.getMessage());
            }
        }
        com.auction.client.network.ServerEventListener.getActiveInstance().setOnBidUpdated(null);
    }

    private void updateDynamicMetaFields(AuctionItem item) {
        if (item == null) return;
        
        // 1. Status badge & Place Bid form active states
        if (badgeLabel != null) {
            if (item.isClosed()) {
                badgeLabel.setText("● CLOSED");
                badgeLabel.getStyleClass().setAll("label", "al-badge-upcoming");
                if (placeBidButton != null) placeBidButton.setDisable(true);
                if (bidAmountField != null) bidAmountField.setDisable(true);
            } else if (item.isPending()) {
                badgeLabel.setText("🕐 UPCOMING");
                badgeLabel.getStyleClass().setAll("label", "al-badge-upcoming");
                if (placeBidButton != null) placeBidButton.setDisable(true);
                if (bidAmountField != null) bidAmountField.setDisable(true);
            } else if (item.isEndingSoon()) {
                badgeLabel.setText("⏰ ENDING SOON");
                badgeLabel.getStyleClass().setAll("label", "al-badge-ending");
                if (placeBidButton != null) placeBidButton.setDisable(false);
                if (bidAmountField != null) bidAmountField.setDisable(false);
            } else {
                badgeLabel.setText("● LIVE");
                badgeLabel.getStyleClass().setAll("label", "al-badge-live");
                if (placeBidButton != null) placeBidButton.setDisable(false);
                if (bidAmountField != null) bidAmountField.setDisable(false);
            }
        }

        // 2. Bidders Participating count (unique bidders in bidHistory)
        if (biddersCountLabel != null) {
            List<String> history = item.getBidHistory();
            int uniqueBidders = 0;
            if (history != null) {
                java.util.Set<String> bidders = new java.util.HashSet<>();
                for (String entry : history) {
                    String[] parts = entry.split("  →  ");
                    if (parts.length > 0) {
                        bidders.add(parts[0].trim());
                    }
                }
                uniqueBidders = bidders.size();
            }
            biddersCountLabel.setText(uniqueBidders + " participating");
        }

        // 3. Reserve Price Met (based on whether any bids are placed)
        if (reserveMetLabel != null) {
            boolean reserveMet = (item.getTotalBids() > 0);
            reserveMetLabel.setText(reserveMet ? "✔ Yes" : "No");
            reserveMetLabel.setStyle(reserveMet ? "-fx-text-fill: #4ade80;" : "-fx-text-fill: #8a8272;");
        }

        // 4. Momentum percentage change from start price
        if (momentumLabel != null) {
            double startPrice = item.getStartingPrice();
            double currPrice = item.getCurrentPrice();
            if (startPrice > 0 && currPrice >= startPrice) {
                double percent = ((currPrice - startPrice) / startPrice) * 100;
                if (percent > 0) {
                    momentumLabel.setText(String.format("+%.1f%% overall", percent));
                    momentumLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold;");
                } else {
                    momentumLabel.setText("0.0% change");
                    momentumLabel.setStyle("-fx-text-fill: #8a8272;");
                }
            } else {
                momentumLabel.setText("—");
                momentumLabel.setStyle("-fx-text-fill: #8a8272;");
            }
        }
    }

    // ── Utilities ─────────────────────────────────────────────
    private String formatTime(int seconds) {
        if (seconds <= 0) return "00:00:00";
        return String.format("%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}