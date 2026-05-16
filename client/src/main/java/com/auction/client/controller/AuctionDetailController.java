package com.auction.client.controller;

import java.util.Timer;
import java.util.TimerTask;

import com.auction.client.model.AuctionItem;
import com.auction.client.util.NavigationUtils;
import com.auction.client.viewmodel.AuctionDetailViewModel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AuctionDetailController {

    @FXML private TextField searchField;
    @FXML private Label    currentBidLabel;
    @FXML private Label    timeRemainingLabel;
    @FXML private Label    titleLabel;
    @FXML private Label    subtitleLabel;
    @FXML private Button   placeBidButton;
    @FXML private Button   btnBack;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private TextField bidAmountField;

    private final AuctionDetailViewModel viewModel = new AuctionDetailViewModel();
    private Timer countdownTimer;

    @FXML
    public void initialize() {
        if (btnBack != null) {
            btnBack.setOnAction(e -> {
                stopTimer();
                NavigationUtils.navigateTo(
                    "/com/auction/client/view/AuctionList.fxml", "Live Auctions");
            });
        }
    }

    public void setAuctionItem(AuctionItem item) {
        viewModel.setItem(item);

        if (titleLabel != null)         titleLabel.setText(viewModel.getDisplayTitle());
        if (subtitleLabel != null)      subtitleLabel.setText(viewModel.getDisplaySubtitle());
        if (currentBidLabel != null)    currentBidLabel.setText(viewModel.getDisplayPrice());
        if (timeRemainingLabel != null) timeRemainingLabel.setText(viewModel.getDisplayTimeRemaining());

        setupChart();
        startCountdownTimer();
    }

    @FXML
    private void handlePlaceBid() {
        AuctionItem item = viewModel.getItem();
        if (item != null) {
            stopTimer();
            NavigationUtils.navigateToBidScreen(item);
        }
    }

    private void startCountdownTimer() {
        stopTimer(); // cancel any existing timer first
        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    AuctionItem item = viewModel.getItem();
                    if (item == null || timeRemainingLabel == null) return;

                    int seconds = item.secondsLeft();
                    timeRemainingLabel.setText(formatTime(seconds));

                    // Switch to red style when ending soon
                    if (seconds < 900) {
                        timeRemainingLabel.getStyleClass().setAll("ad-timer-ending");
                    } else {
                        timeRemainingLabel.getStyleClass().setAll("ad-timer-value");
                    }

                    // Stop ticking when auction ends
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

    private String formatTime(int seconds) {
        if (seconds <= 0) return "00:00:00";
        return String.format("%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}