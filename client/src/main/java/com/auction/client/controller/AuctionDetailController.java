package com.auction.client.controller;

import com.auction.client.model.AuctionItem;
import com.auction.client.util.NavigationUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AuctionDetailController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label timerLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label statusLabel;
    @FXML private ImageView productImage;
    @FXML private Button placeBidButton;
    @FXML private Button watchButton;
    @FXML private Button btnBack;

    private AuctionItem currentItem;

    @FXML
    public void initialize() {
        // Setup button events
        btnBack.setOnAction(e -> NavigationUtils.navigateTo("/com/auction/client/view/AuctionList.fxml", "Auction List"));
        placeBidButton.setOnAction(e -> handlePlaceBid());
        watchButton.setOnAction(e -> handleWatch());
    }

    /**
     * Nhận dữ liệu từ AuctionList và hiển thị
     */
    public void setAuctionItem(AuctionItem item) {
        this.currentItem = item;

        titleLabel.setText(item.getTitle());
        subtitleLabel.setText(item.getSubtitle());
        currentPriceLabel.setText(String.format("$%,.0f", item.getCurrentBid()));
        descriptionLabel.setText(item.getSubtitle() + "\n\nThis is a premium collectible item with exceptional value and historical significance.");
        
        // Set status
        statusLabel.setText(item.getStatus().equals("ENDING_SOON") ? "ENDING SOON" : "● LIVE");
        statusLabel.getStyleClass().setAll(item.getStatus().equals("ENDING_SOON") ? "al-badge-ending" : "al-badge-live");

        // Set image (demo)
        try {
            productImage.setImage(new Image(getClass().getResourceAsStream("/com/auction/client/image/placeholder.jpg")));
        } catch (Exception e) {
            // fallback
        }

        // Timer simulation
        timerLabel.setText("12h 34m 56s");
    }

    private void handlePlaceBid() {
        if (currentItem != null) {
            NavigationUtils.navigateTo("/com/auction/client/view/Bidding.fxml", "Place Bid - " + currentItem.getTitle());
        }
    }

    private void handleWatch() {
        System.out.println("Added to watchlist: " + (currentItem != null ? currentItem.getTitle() : ""));
        // TODO: Thêm vào danh sách theo dõi
    }
}