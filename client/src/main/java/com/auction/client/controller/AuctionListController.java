package com.auction.client.controller;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.auction.client.model.AuctionItem;
import com.auction.client.sessions.UserSession;
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

    // ── Navbar ────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label     userInitialLabel;
    @FXML private Button    btnLogout;

    // ── Sidebar Filters ───────────────────────────────────────
    @FXML private CheckBox catFineArt;
    @FXML private CheckBox catLuxuryWatches;
    @FXML private CheckBox catClassicCars;
    @FXML private CheckBox catJewelry;
    @FXML private TextField priceMin;
    @FXML private TextField priceMax;
    @FXML private Button    statusLive;
    @FXML private Button    statusUpcoming;
    @FXML private Button    statusEndingSoon;
    @FXML private Button    applyFilterBtn;

    // ── Main Content ──────────────────────────────────────────
    @FXML private Label            countLabel;
    @FXML private ComboBox<String> sortCombo;
    @FXML private GridPane         cardsGrid;

    private Timer clockTimer;

    // ── Lifecycle ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        loadUserInfo();
        setupSearch();
        setupSortCombo();
        setupStatusButtons();
        setupApplyFilter();

        viewModel.loadData();
        refreshCards();
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

    // ── Logout ────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        if (clockTimer != null) clockTimer.cancel();
        NavigationUtils.logout();
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
        if (statusLive      != null) statusLive.setOnAction(e -> setActiveStatus(statusLive));
        if (statusUpcoming  != null) statusUpcoming.setOnAction(e -> setActiveStatus(statusUpcoming));
        if (statusEndingSoon!= null) statusEndingSoon.setOnAction(e -> setActiveStatus(statusEndingSoon));
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
            else if (activeBtn == statusUpcoming)     viewModel.setFilterStatus("PENDING");
            else if (activeBtn == statusEndingSoon)   viewModel.setFilterStatus("ENDING_SOON");
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
        if (catFineArt.isSelected())       cats.add("Art");
        if (catLuxuryWatches.isSelected()) cats.add("Watches");
        if (catClassicCars.isSelected())   cats.add("Vehicles");
        if (catJewelry.isSelected())       cats.add("Jewellery");
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
        if ("ENDING_SOON".equals(displayStatus)) {
            badgeText = "⏰ ENDING SOON"; badgeStyle = "al-badge-ending";
        } else if (item.isPending()) {
            badgeText = "🕐 UPCOMING";   badgeStyle = "al-badge-upcoming";
        } else {
            badgeText = "● LIVE";        badgeStyle = "al-badge-live";
        }
        Label badge = new Label(badgeText);
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

        // Click card body to open detail
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
                    for (AuctionItem item : viewModel.getFilteredItems()) {
                        Label lbl = (Label) cardsGrid.lookup("#timer_" + item.getAuctionId());
                        if (lbl != null) {
                            int remaining = item.secondsLeft();
                            lbl.setText(formatTime(remaining));
                            if (remaining < 900)
                                lbl.getStyleClass().setAll("al-timer-ending");
                        }
                    }
                });
            }
        }, 1000, 1000);
    }
    // ── Utilities ─────────────────────────────────────────────
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
    private String formatTime(int seconds) {
        if (seconds <= 0) return "00:00:00";
        return String.format("%02d:%02d:%02d",
            seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}