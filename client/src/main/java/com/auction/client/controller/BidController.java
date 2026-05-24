package com.auction.client.controller;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

import com.auction.client.model.AuctionItem;
import com.auction.client.model.User;
import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;
import com.auction.client.network.ClientMessageSender;
import com.auction.client.network.ServerEventListener;
import com.auction.common.protocol.MessageEnvelope;
import com.auction.common.protocol.MessageType;
import com.auction.common.protocol.ProtocolMapper;
import com.auction.common.protocol.ErrorPayload;

import javafx.application.Platform;
import javafx.fxml.FXML;
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

    @FXML private ListView<String> bidHistoryList;
    @FXML private Label            bidCountLabel;

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
        startCountdownTimer();
    }

    // ── Balance label ─────────────────────────────────────────
    private void refreshBalanceLabel() {
        if (balanceLabel != null) {
            double bal = UserSession.getInstance().getBalance();
            balanceLabel.setText(fmt(bal));
            if (bal >= 10_000) {
                balanceLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else if (bal >= 1_000) {
                balanceLabel.setStyle("-fx-text-fill: #f0b429; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else {
                balanceLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-font-weight: bold;");
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

                        String entry = bidder + "  →  " + fmt(amount);
                        if (currentItem.getBidHistory() == null) {
                            currentItem.setBidHistory(new java.util.ArrayList<>());
                        }
                        currentItem.getBidHistory().add(0, entry);

                        // Reload list
                        seedMockHistory(currentItem);

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
        String entry = (UserSession.getInstance().isLoggedIn()
                ? UserSession.getInstance().getCurrentUser().getUsername() : "You")
                + "  →  " + fmt(t.amount);
        // Already added directly in handleConfirmBid; avoid duplicate
        if (!bidHistoryList.getItems().isEmpty()
                && bidHistoryList.getItems().get(0).equals(entry)) return;
        bidHistoryList.getItems().add(0, entry);
        bidCountLabel.setText(bidHistoryList.getItems().size() + " bids");
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

    // ── Cleanup ───────────────────────────────────────────────
    private void cleanup() {
        stopTimer();
        if (balanceListener != null)
            UserSession.getInstance().removeBalanceListener(balanceListener);
        if (transactionListener != null)
            UserSession.getInstance().removeTransactionListener(transactionListener);
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
}
