package com.auction.client.controller;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.auction.client.model.AuctionItem;
import com.auction.client.sessions.UserSession;
import com.auction.client.viewmodel.AuctionListViewModel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuctionListController {

    // ── ViewModel ─────────────────────────────────────────────
    private final AuctionListViewModel viewModel = new AuctionListViewModel();

    // ── Navbar ────────────────────────────────────────────────
    @FXML private Button navHome;
    @FXML private Button navSelling;
    @FXML private Button navHistory;

    // ── Sidebar ───────────────────────────────────────────────
    @FXML private Label    userNameLabel;
    @FXML private Button   sideHome;
    @FXML private Button   sideActiveBids;
    @FXML private Button   sideInventory;
    @FXML private CheckBox catArt;
    @FXML private CheckBox catElectronics;
    @FXML private CheckBox catVehicles;
    @FXML private CheckBox catWatches;
    @FXML private Button   statusAll;
    @FXML private Button   statusLive;
    @FXML private Button   statusEnding;
    @FXML private TextField priceMin;
    @FXML private TextField priceMax;
    @FXML private Button   applyFilterBtn;

    // ── Main ──────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private Label            countLabel;
    @FXML private ComboBox<String> sortCombo;
    @FXML private GridPane         cardsGrid;
    @FXML private ScrollPane       scrollPane;

    private Timer clockTimer;

    // ── Init ──────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupUserInfo();
        setupSort();
        setupSearch();
        setupNavigation();
        setupStatusButtons();
        viewModel.loadData();
        renderCards(viewModel.getFilteredItems());
        startCountdownTimers();
    }

    // ── User info từ UserSession ──────────────────────────────
    private void setupUserInfo() {
        UserSession session = UserSession.getInstance();
        if (session.isLoggedIn() && userNameLabel != null) {
            userNameLabel.setText(session.getCurrentUser().getFullName());
        }
    }

    // ── Sort ──────────────────────────────────────────────────
    private void setupSort() {
        sortCombo.setItems(FXCollections.observableArrayList(
            "Sort by: Newest", "Sort by: Price ↑",
            "Sort by: Price ↓", "Ending Soonest"
        ));
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setOnAction(e -> {
            String sel = sortCombo.getValue();
            String sortKey = "NEWEST";
            if ("Sort by: Price ↑".equals(sel)) sortKey = "PRICE_ASC";
            else if ("Sort by: Price ↓".equals(sel)) sortKey = "PRICE_DESC";
            else if ("Ending Soonest".equals(sel))   sortKey = "ENDING_SOON";
            viewModel.setSortBy(sortKey);
            renderCards(viewModel.applyFilters());
        });
    }

    // ── Search ────────────────────────────────────────────────
    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, keyword) -> {
            viewModel.setKeyword(keyword);
            renderCards(viewModel.applyFilters());
        });
    }

    // ── Navigation ────────────────────────────────────────────
    private void setupNavigation() {
        UserSession session = UserSession.getInstance();
        if (session.isSeller()) {
            navSelling.setOnAction(e ->
                navigateTo("/com/auction/client/view/SellerDashboard.fxml", "Selling"));
        }
        if (session.isAdmin()) {
            navSelling.setText("Admin Panel");
            navSelling.setOnAction(e ->
                navigateTo("/com/auction/client/view/AdminDashboard.fxml", "Admin"));
        }
        sideActiveBids.setOnAction(e ->
            navigateTo("/com/auction/client/view/ActiveBids.fxml", "Active Bids"));
        sideInventory.setOnAction(e ->
            navigateTo("/com/auction/client/view/Inventory.fxml", "My Inventory"));
    }

    // ── Status buttons ────────────────────────────────────────
    private void setupStatusButtons() {
        statusAll.setOnAction(e -> {
            viewModel.setFilterStatus("ALL");
            refreshStatusStyles("ALL");
            renderCards(viewModel.applyFilters());
        });
        statusLive.setOnAction(e -> {
            viewModel.setFilterStatus("LIVE");
            refreshStatusStyles("LIVE");
            renderCards(viewModel.applyFilters());
        });
        statusEnding.setOnAction(e -> {
            viewModel.setFilterStatus("ENDING_SOON");
            refreshStatusStyles("ENDING_SOON");
            renderCards(viewModel.applyFilters());
        });
    }

    private void refreshStatusStyles(String active) {
        statusAll.getStyleClass().setAll(
            "ALL".equals(active) ? "al-status-btn-active" : "al-status-btn");
        statusLive.getStyleClass().setAll(
            "LIVE".equals(active) ? "al-status-btn-active" : "al-status-btn");
        statusEnding.getStyleClass().setAll(
            "ENDING_SOON".equals(active) ? "al-status-btn-ending" : "al-status-btn");
    }

    // ── Apply filter button ───────────────────────────────────
    @FXML
    private void handleApplyFilter() {
        // Category
        if (catArt.isSelected() && catElectronics.isSelected()
                && catVehicles.isSelected() && catWatches.isSelected()) {
            viewModel.setFilterCategory("ALL");
        } else if (catArt.isSelected())
            viewModel.setFilterCategory("Art");
        else if (catElectronics.isSelected())
            viewModel.setFilterCategory("Electronics");
        else if (catVehicles.isSelected())
            viewModel.setFilterCategory("Vehicles");
        else if (catWatches.isSelected())
            viewModel.setFilterCategory("Watches");
        else
            viewModel.setFilterCategory("ALL");

        // Price range
        double min = 0, max = 0;
        try { min = Double.parseDouble(priceMin.getText().trim()); }
        catch (NumberFormatException ignored) {}
        try { max = Double.parseDouble(priceMax.getText().trim()); }
        catch (NumberFormatException ignored) {}
        viewModel.setPriceRange(min, max);

        renderCards(viewModel.applyFilters());
    }

    // ── Render cards ──────────────────────────────────────────
    private void renderCards(List<AuctionItem> items) {
        cardsGrid.getChildren().clear();
        countLabel.setText("Showing " + viewModel.getFilteredCount()
            + " of " + viewModel.getTotalCount() + " items");

        int col = 0, row = 0;
        for (AuctionItem item : items) {
            VBox card = buildCard(item);
            GridPane.setColumnIndex(card, col);
            GridPane.setRowIndex(card, row);
            cardsGrid.getChildren().add(card);
            if (++col == 3) { col = 0; row++; }
        }

        if (items.isEmpty()) {
            Label empty = new Label("No auctions match your filters.");
            empty.setStyle("-fx-text-fill:#555e7a; -fx-font-size:14px; -fx-font-family:'Arial';");
            GridPane.setColumnIndex(empty, 0);
            GridPane.setRowIndex(empty, 0);
            cardsGrid.getChildren().add(empty);
        }
    }

    // ── Build card ────────────────────────────────────────────
    private VBox buildCard(AuctionItem item) {
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(200);
        String bg = "#1a1a1a";
        if ("Vehicles".equals(item.getCategory()))    bg = "#1a2035";
        else if ("Watches".equals(item.getCategory())) bg = "#1a1a2e";
        else if ("Art".equals(item.getCategory()))     bg = "#0d1f2d";
        else if ("Electronics".equals(item.getCategory())) bg = "#1a2a1a";
        imgPane.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12 12 0 0;");

        String emoji = "🖼";
        if ("Vehicles".equals(item.getCategory()))    emoji = "🚗";
        else if ("Watches".equals(item.getCategory())) emoji = "⌚";
        else if ("Art".equals(item.getCategory()))     emoji = "🎨";
        else if ("Electronics".equals(item.getCategory())) emoji = "💻";

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size:64px;");

        boolean ending = item.isEndingSoon() || item.secondsLeft() < 900;
        Label badge = new Label(ending ? "ENDING SOON" : "● LIVE");
        badge.getStyleClass().add(ending ? "al-badge-ending" : "al-badge-live");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(12, 0, 0, 12));

        Button heart = new Button("♡");
        heart.getStyleClass().add("al-btn-heart");
        StackPane.setAlignment(heart, Pos.TOP_RIGHT);
        StackPane.setMargin(heart, new Insets(10, 10, 0, 0));
        imgPane.getChildren().addAll(emojiLbl, badge, heart);

        VBox body = new VBox(4);
        body.getStyleClass().add("al-card-body");

        Label titleLbl = new Label(item.getTitle());
        titleLbl.getStyleClass().add("al-card-title");
        titleLbl.setWrapText(true);

        Label subLbl = new Label(item.getSubtitle());
        subLbl.getStyleClass().add("al-card-subtitle");
        VBox.setMargin(subLbl, new Insets(0, 0, 10, 0));

        HBox bidRow = new HBox();
        VBox.setMargin(bidRow, new Insets(0, 0, 12, 0));

        VBox bidBox = new VBox(2);
        Label bidLbl = new Label("CURRENT BID");
        bidLbl.getStyleClass().add("al-bid-label");
        Label bidAmt = new Label(String.format("$%,.0f", item.getCurrentBid()));
        bidAmt.getStyleClass().add("al-bid-amount");
        bidBox.getChildren().addAll(bidLbl, bidAmt);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox timerBox = new VBox(2);
        timerBox.setAlignment(Pos.CENTER_RIGHT);
        Label timerLbl = new Label("ENDS IN");
        timerLbl.getStyleClass().add("al-timer-label");
        Label timerVal = new Label(formatTime(item.secondsLeft()));
        timerVal.setId("timerCard_" + item.getId());
        timerVal.getStyleClass().add(item.secondsLeft() < 900 ? "al-timer-ending" : "al-timer-value");
        timerBox.getChildren().addAll(timerLbl, timerVal);
        bidRow.getChildren().addAll(bidBox, spacer, timerBox);

        Button bidBtn = new Button("PLACE BID");
        bidBtn.getStyleClass().add("al-btn-bid");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setPrefHeight(40);
        bidBtn.setOnAction(e -> handlePlaceBid(item));

        body.getChildren().addAll(titleLbl, subLbl, bidRow, bidBtn);

        VBox card = new VBox(0);
        card.getStyleClass().add("al-card");
        card.setPrefWidth(310);
        card.getChildren().addAll(imgPane, body);
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Button))
                handleOpenDetail(item);
        });
        return card;
    }

    // ── Countdown ─────────────────────────────────────────────
    private void startCountdownTimers() {
        clockTimer = new Timer(true);
        clockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> refreshTimerLabels());
            }
        }, 1000, 1000);
    }

    private void refreshTimerLabels() {
        for (AuctionItem item : viewModel.getFilteredItems()) {
            Node node = cardsGrid.lookup("#timerCard_" + item.getId());
            if (node instanceof Label) {
                Label lbl = (Label) node;
                int secs = item.secondsLeft();
                lbl.setText(formatTime(secs));
                lbl.getStyleClass().setAll(secs < 900 ? "al-timer-ending" : "al-timer-value");
            }
        }
    }

    private String formatTime(int secs) {
        if (secs >= 86400)
            return (secs / 86400) + "d " + String.format("%02dh", (secs % 86400) / 3600);
        return String.format("%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60);
    }

    // ── Actions ───────────────────────────────────────────────
    private void handlePlaceBid(AuctionItem item) {
        navigateTo("/com/auction/client/view/Bidding.fxml", "Place Bid");
    }

    private void handleOpenDetail(AuctionItem item) {
        navigateTo("/com/auction/client/view/AuctionDetail.fxml", item.getTitle());
    }

    @FXML
    private void handlePlaceBid() {
        navigateTo("/com/auction/client/view/Bidding.fxml", "Place Bid");
    }

    private void navigateTo(String path, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/com/auction/client/css/style.css").toExternalForm()
            );
            Stage stage = (Stage) cardsGrid.getScene().getWindow();
            stage.setTitle(title + " — Auction Pro");
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
