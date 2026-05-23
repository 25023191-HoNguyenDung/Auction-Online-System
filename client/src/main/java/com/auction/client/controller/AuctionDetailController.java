package com.auction.client.controller;

import java.util.Timer;
import java.util.TimerTask;
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

                    if (seconds <= 0) stopTimer();
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
        if (priceChart == null) return;
        priceChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Price Trend");

        for (AuctionDetailViewModel.ChartPoint p : viewModel.getPriceHistory()) {
            series.getData().add(new XYChart.Data<>(p.label, p.price));
        }

        priceChart.getData().add(series);
    }

    // ── Cleanup ───────────────────────────────────────────────
    private void cleanup() {
        stopTimer();
        if (balanceListener != null)
            UserSession.getInstance().removeBalanceListener(balanceListener);
    }

    // ── Utilities ─────────────────────────────────────────────
    private String formatTime(int seconds) {
        if (seconds <= 0) return "00:00:00";
        return String.format("%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}