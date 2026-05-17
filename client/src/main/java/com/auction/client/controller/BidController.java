package com.auction.client.controller;

import java.util.Timer;
import java.util.TimerTask;

import com.auction.client.model.AuctionItem;
import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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

    @FXML private ListView<String> bidHistoryList;
    @FXML private Label            bidCountLabel;

    private AuctionItem currentItem;
    private double currentBid = 0;
    private static final double MOCK_BALANCE = 50_000.0;
    private Timer countdownTimer;

    // ── Lifecycle ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (btnBack != null) {
            btnBack.setOnAction(e -> {
                stopTimer();
                if (currentItem != null) {
                    NavigationUtils.navigateToAuctionDetail(currentItem);
                } else {
                    NavigationUtils.navigateTo(
                        "/com/auction/client/view/AuctionList.fxml", "Live Auctions");
                }
            });
        }
        setupBidHistoryList();
    }

    // ── Called from NavigationUtils after FXML load ───────────
    public void setAuctionItem(AuctionItem item) {
        this.currentItem = item;
        this.currentBid  = item.getCurrentPrice();

        itemEmojiLabel.setText(emojiFor(item.getCategory()));
        itemTitleLabel.setText(item.getItemName());
        itemSubtitleLabel.setText(item.getDescription() != null ? item.getDescription() : "");
        currentBidLabel.setText(fmt(currentBid));
        timeRemainingLabel.setText(formatTime(item.secondsLeft()));
        totalBidsLabel.setText(String.valueOf(item.getTotalBids()));
        sellerLabel.setText(item.getSellerName() != null ? item.getSellerName() : "—");

        if (item.isEndingSoon()) {
            badgeLabel.setText("⏰ ENDING SOON");
            badgeLabel.getStyleClass().setAll("al-badge-ending");
        } else if (item.isPending()) {
            badgeLabel.setText("🕐 UPCOMING");
            badgeLabel.getStyleClass().setAll("al-badge-upcoming");
        } else {
            badgeLabel.setText("● LIVE");
            badgeLabel.getStyleClass().setAll("al-badge-live");
        }

        double minBid = currentBid + 1;
        minBidLabel.setText(fmt(minBid));
        balanceLabel.setText(fmt(MOCK_BALANCE));
        bidAmountField.setPromptText(fmt(minBid));

        seedMockHistory(item);
        startCountdownTimer();
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

                    if (seconds < 900) {
                        timeRemainingLabel.getStyleClass().setAll("ad-timer-ending");
                    } else {
                        timeRemainingLabel.getStyleClass().setAll("ad-timer-value");
                    }

                    if (seconds <= 0) {
                        stopTimer();
                        if (confirmBidButton != null) confirmBidButton.setDisable(true);
                        showError("Auction has ended.");
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
        String raw = src.getText().replace("+$", "").replace(",", "").trim();
        try {
            double increment = Double.parseDouble(raw);
            bidAmountField.setText(String.format("%.0f", currentBid + increment));
            hideMessage();
        } catch (NumberFormatException ignored) {}
    }

    // ── Confirm bid ───────────────────────────────────────────
    @FXML
    private void handleConfirmBid() {
        hideMessage();

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
        if (amount > MOCK_BALANCE) {
            showError("Insufficient balance. Your balance is " + fmt(MOCK_BALANCE) + ".");
            return;
        }

        currentBid = amount;
        currentBidLabel.setText(fmt(currentBid));
        minBidLabel.setText(fmt(currentBid + 1));
        bidAmountField.clear();

        String bidder = UserSession.getInstance().isLoggedIn()
            ? UserSession.getInstance().getCurrentUser().getUsername()
            : "You";

        bidHistoryList.getItems().add(0, bidder + "  →  " + fmt(amount));
        bidCountLabel.setText(bidHistoryList.getItems().size() + " bids");
        showSuccess("Bid of " + fmt(amount) + " placed successfully!");
        System.out.println("✅ Bid placed: " + fmt(amount) + " on " + currentItem.getItemName());
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

                Label amount = new Label(parts.length > 1 ? parts[1] : "");
                amount.setStyle("-fx-text-fill: #f0b429; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Arial';");

                if (getIndex() == 0) {
                    bidder.setStyle(bidder.getStyle() + " -fx-text-fill: #f5f0e6;");
                    amount.setStyle(amount.getStyle() + " -fx-text-fill: #4ade80;");
                }

                row.getChildren().addAll(bidder, spacer, amount);
                setGraphic(row);
                setText(null);
                setStyle("-fx-background-color: transparent;");
            }
        });
    }

    private void seedMockHistory(AuctionItem item) {
        bidHistoryList.getItems().clear();
        double price = item.getCurrentPrice();
        String[][] mocks = {
            {"Sterling House", fmt(price)},
            {"@bidder_99",     fmt(price * 0.97)},
            {"@luxcollector",  fmt(price * 0.94)},
            {"@marcus_g",      fmt(price * 0.90)},
        };
        for (String[] m : mocks) bidHistoryList.getItems().add(m[0] + "  →  " + m[1]);
        bidCountLabel.setText(bidHistoryList.getItems().size() + " bids");
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
            case "vehicles"            -> "🏎️";
            case "watches"             -> "⌚";
            case "art"                 -> "🖼️";
            case "jewelry","jewellery" -> "💎";
            case "electronics"         -> "💻";
            default                    -> "⭐";
        };
    }
}