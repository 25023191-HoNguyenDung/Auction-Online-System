package com.auction.client.controller;

import com.auction.client.sessions.AccountService;
import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class BidHistoryController {
    @FXML private Label userInitialLabel;
    @FXML private Label balanceLabel;
    @FXML private ListView<String> bidHistoryList;
    @FXML private ListView<String> paymentHistoryList;
    @FXML private Label bidEmptyLabel;
    @FXML private Label paymentEmptyLabel;
    @FXML private Button btnLogout;

    private final AccountService accountService = AccountService.getInstance();

    @FXML
    public void initialize() {
        loadUserInfo();
        balanceLabel.setText(fmt(accountService.getBalance()));
        bidHistoryList.setItems(accountService.getBidHistory());
        paymentHistoryList.setItems(accountService.getPaymentHistory());
        refreshEmptyStates();
    }

    @FXML
    private void handleAuctionsNav() {
        NavigationUtils.navigateTo("/com/auction/client/view/AuctionList.fxml", "Live Auctions");
    }

    @FXML
    private void handleLogout() {
        NavigationUtils.logout();
    }

    private void loadUserInfo() {
        if (userInitialLabel == null || !UserSession.getInstance().isLoggedIn()) return;
        String name = UserSession.getInstance().getCurrentUser().getUsername();
        userInitialLabel.setText(name != null && !name.isEmpty()
            ? String.valueOf(name.charAt(0)).toUpperCase()
            : "?");
    }

    private void refreshEmptyStates() {
        if (bidEmptyLabel != null) bidEmptyLabel.setVisible(accountService.getBidHistory().isEmpty());
        if (paymentEmptyLabel != null) paymentEmptyLabel.setVisible(accountService.getPaymentHistory().isEmpty());
    }

    private String fmt(double value) {
        return String.format("$%,.0f", value);
    }
}
