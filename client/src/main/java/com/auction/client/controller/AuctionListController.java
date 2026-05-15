package com.auction.client.controller;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.auction.client.model.AuctionItem;
import com.auction.client.util.NavigationUtils;
import com.auction.client.viewmodel.AuctionListViewModel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    // Navbar
    @FXML private TextField searchField;

    // Sidebar Filters
    @FXML private CheckBox catFineArt;
    @FXML private CheckBox catLuxuryWatches;
    @FXML private CheckBox catClassicCars;
    @FXML private CheckBox catJewelry;

    @FXML private TextField priceMin;
    @FXML private TextField priceMax;

    @FXML private Button statusLive;
    @FXML private Button statusUpcoming;
    @FXML private Button statusEndingSoon;

    @FXML private Button applyFilterBtn;

    // Main Content
    @FXML private Label countLabel;
    @FXML private ComboBox<String> sortCombo;
    @FXML private GridPane cardsGrid;

    private Timer clockTimer;

    @FXML
    public void initialize() {
        setupSearch();
        setupSortCombo();
        setupStatusButtons();
        setupApplyFilter();

        viewModel.loadData();
        refreshCards();

        startCountdownTimer();
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, newText) -> {
                viewModel.setKeyword(newText);
                refreshCards();
            });
        }
    }

    private void setupSortCombo() {
        if (sortCombo != null) {
            sortCombo.getItems().addAll(
                "Newest", 
                "Price: Low to High", 
                "Price: High to Low", 
                "Ending Soon"
            );
            sortCombo.getSelectionModel().selectFirst();

            sortCombo.setOnAction(e -> refreshCards());
        }
    }

    private void setupStatusButtons() {
    if (statusLive != null) {
        statusLive.setOnAction(e -> setActiveStatus(statusLive));
    }
    if (statusUpcoming != null) {
        statusUpcoming.setOnAction(e -> setActiveStatus(statusUpcoming));
    }
    if (statusEndingSoon != null) {
        statusEndingSoon.setOnAction(e -> setActiveStatus(statusEndingSoon));
    }
}

    private void setActiveStatus(Button activeBtn) {
        // Reset all
        statusLive.getStyleClass().remove("active");
        statusUpcoming.getStyleClass().remove("active");
        statusEndingSoon.getStyleClass().remove("active");

        // Activate selected
        activeBtn.getStyleClass().add("active");

        // Set filter
        if (activeBtn == statusLive) {
            viewModel.setFilterStatus("LIVE");
        } else if (activeBtn == statusUpcoming) {
            viewModel.setFilterStatus("PENDING");     // or "UPCOMING"
        } else if (activeBtn == statusEndingSoon) {
            viewModel.setFilterStatus("ENDING_SOON");
        }

        refreshCards();
    }

    private void setupApplyFilter() {
        if (applyFilterBtn != null) {
            applyFilterBtn.setOnAction(e -> handleApplyFilter());
        }
    }

    @FXML
    private void handleApplyFilter() {
        // Price Range
        double min = 0;
        double max = Double.MAX_VALUE;
        try { min = Double.parseDouble(priceMin.getText().trim()); } catch (Exception ignored) {}
        try { max = Double.parseDouble(priceMax.getText().trim()); } catch (Exception ignored) {}

        viewModel.setPriceRange(min, max);

        // Simple category filter (can be improved later)
        String category = "ALL";
        if (!catFineArt.isSelected() && !catLuxuryWatches.isSelected() && 
            !catClassicCars.isSelected() && !catJewelry.isSelected()) {
            category = "ALL";
        }

        viewModel.setFilterCategory(category);
        refreshCards();
    }

    private void refreshCards() {
        List<AuctionItem> items = viewModel.applyFilters();

        countLabel.setText("Showing " + items.size() + " of " + viewModel.getTotalCount() + " items");

        cardsGrid.getChildren().clear();

        int col = 0, row = 0;
        for (AuctionItem item : items) {
            VBox card = buildAureateCard(item);
            GridPane.setColumnIndex(card, col);
            GridPane.setRowIndex(card, row);
            cardsGrid.getChildren().add(card);

            col++;
            if (col == 3) {
                col = 0;
                row++;
            }
        }
    }

    private VBox buildAureateCard(AuctionItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("al-card");
        card.setPrefWidth(340);

        // Image Area
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(220);
        imagePane.setStyle("-fx-background-color: #1e1c15; -fx-background-radius: 12 12 0 0;");

        Label emoji = new Label(getEmojiForCategory(item.getCategory()));
        emoji.setStyle("-fx-font-size: 80px;");
        imagePane.getChildren().add(emoji);

        // Status Badge
        String displayStatus = item.getDisplayStatus(); // LIVE | ENDING_SOON | PENDING
        String badgeText;
        String badgeStyle;
        if ("ENDING_SOON".equals(displayStatus)) {
            badgeText  = "⏰ ENDING SOON";
            badgeStyle = "al-badge-ending";
        } else if ("PENDING".equals(displayStatus) || item.isPending()) {
            badgeText  = "🕐 UPCOMING";
            badgeStyle = "al-badge-upcoming";
        } else {
            badgeText  = "● LIVE";
            badgeStyle = "al-badge-live";
        }
        Label badge = new Label(badgeText);
        badge.getStyleClass().add(badgeStyle);
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(12, 0, 0, 12));
        imagePane.getChildren().add(badge);

        // Card Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(16));
        body.getStyleClass().add("al-card-body");

        Label title = new Label(item.getItemName());
        title.getStyleClass().add("al-card-title");
        title.setWrapText(true);

        Label subtitle = new Label(item.getDescription());
        subtitle.getStyleClass().add("al-card-subtitle");
        subtitle.setWrapText(true);

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
        bidButton.setOnAction(e -> handlePlaceBid(item));

        body.getChildren().addAll(title, subtitle, bidRow, bidButton);

        card.getChildren().addAll(imagePane, body);

        // Click on card (except button) to open detail
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Button)) {
                handleOpenDetail(item);
            }
        });

        return card;
    }

    private String getEmojiForCategory(String category) {
        return switch (category.toLowerCase()) {
            case "vehicles" -> "🏎️";
            case "watches" -> "⌚";
            case "art" -> "🖼️";
            case "jewelry", "jewellery" -> "💎";
            default -> "⭐";
        };
    }

    private String formatTime(int seconds) {
        if (seconds <= 0) return "00:00:00";
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void startCountdownTimer() {
        clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(this::updateTimers);
            }

            private void updateTimers() {
                for (AuctionItem item : viewModel.getFilteredItems()) {
                    Label timerLabel = (Label) cardsGrid.lookup("#timer_" + item.getAuctionId());
                    if (timerLabel != null) {
                        int remaining = item.secondsLeft();
                        timerLabel.setText(formatTime(remaining));
                        if (remaining < 900) {
                            timerLabel.getStyleClass().setAll("al-timer-ending");
                        }
                    }
                }
            }
        }, 1000, 1000);
    }

    private void handlePlaceBid(AuctionItem item) {
        System.out.println("Placing bid on: " + item.getItemName());
        NavigationUtils.navigateToAuctionDetail(item);
    }

    private void handleOpenDetail(AuctionItem item) {
        System.out.println("Opening detail for: " + item.getItemName());
        NavigationUtils.navigateToAuctionDetail(item);
    }

    public void shutdown() {
        if (clockTimer != null) {
            clockTimer.cancel();
        }
    }
}