package com.auction.client.controller;

import com.auction.client.model.AuctionItem;
import com.auction.client.util.NavigationUtils;
import com.auction.client.viewmodel.AuctionDetailViewModel;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AuctionDetailController {

    @FXML private TextField searchField;        // navbar search (bound but unused for now)
    @FXML private Label    currentBidLabel;
    @FXML private Label    timeRemainingLabel;
    @FXML private Label    titleLabel;
    @FXML private Label    subtitleLabel;
    @FXML private Button   placeBidButton;
    @FXML private Button   btnBack;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private TextField bidAmountField;

    private final AuctionDetailViewModel viewModel = new AuctionDetailViewModel();

    @FXML
    public void initialize() {
        if (btnBack != null) {
            btnBack.setOnAction(e -> NavigationUtils.navigateTo(
                "/com/auction/client/view/AuctionList.fxml", "Live Auctions"));
        }
    }

    /**
     * Called from AuctionListController after loading this FXML.
     */
    public void setAuctionItem(AuctionItem item) {
        viewModel.setItem(item);

        if (titleLabel != null)         titleLabel.setText(viewModel.getDisplayTitle());
        if (subtitleLabel != null)      subtitleLabel.setText(viewModel.getDisplaySubtitle());
        if (currentBidLabel != null)    currentBidLabel.setText(viewModel.getDisplayPrice());
        if (timeRemainingLabel != null) timeRemainingLabel.setText(viewModel.getDisplayTimeRemaining());

        setupChart();
    }

    @FXML
    private void handlePlaceBid() {
        AuctionItem item = viewModel.getItem();
        if (item != null) {
            System.out.println("Placing bid on auction: " + item.getAuctionId());
            NavigationUtils.navigateTo("/com/auction/client/view/BidScreen.fxml", "Place Bid");
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
}