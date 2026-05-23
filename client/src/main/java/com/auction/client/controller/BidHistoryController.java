package com.auction.client.controller;

import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.Consumer;

/**
 * Shows the full transaction log (bids + deposits + withdrawals) from
 * UserSession.  The list updates in real-time whenever any screen
 * triggers a balance change.
 */
public class BidHistoryController {

    @FXML private ListView<UserSession.Transaction> activityListView;

    private Consumer<UserSession.Transaction> transactionListener;

    // ── Lifecycle ─────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupListView();
        loadFromSession();

        transactionListener = t -> Platform.runLater(() -> upsertTransaction(t));
        UserSession.getInstance().addTransactionListener(transactionListener);
    }

    // ── Load existing transactions ────────────────────────────
    private void loadFromSession() {
        activityListView.getItems().clear();
        List<UserSession.Transaction> all = UserSession.getInstance().getTransactions();
        activityListView.getItems().addAll(all);
    }

    private void upsertTransaction(UserSession.Transaction updated) {
        for (int i = 0; i < activityListView.getItems().size(); i++) {
            UserSession.Transaction current = activityListView.getItems().get(i);
            if (current.kind == updated.kind
                    && current.itemName.equals(updated.itemName)
                    && current.time.equals(updated.time)) {
                activityListView.getItems().set(i, updated);
                return;
            }
        }
        activityListView.getItems().add(0, updated);
    }

    // ── ListView cell factory ─────────────────────────────────
    private void setupListView() {
        activityListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserSession.Transaction entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                // Status badge
                Label badge = new Label(entry.status);
                badge.setStyle(badgeStyle(entry.status));
                badge.setMinWidth(90);

                // Display name
                String displayName = switch (entry.kind) {
                    case DEPOSIT  -> "Deposit";
                    case WITHDRAW -> "Withdrawal";
                    case BID      -> entry.itemName;
                };
                Label item = new Label(displayName);
                item.setStyle("-fx-text-fill: #f5f0e6; -fx-font-size: 13px;" +
                              " -fx-font-weight: bold; -fx-font-family: 'Arial';");
                item.setMaxWidth(300);

                // Amount — green for incoming money, gold for bids, red for withdrawals
                double displayAmount = entry.kind == UserSession.Transaction.Kind.BID
                        ? Math.abs(entry.amount)
                        : entry.amount;
                String amountStr = String.format("$%,.0f", displayAmount);
                if (entry.kind == UserSession.Transaction.Kind.DEPOSIT) {
                    amountStr = "+ " + amountStr;
                } else if (entry.kind == UserSession.Transaction.Kind.WITHDRAW) {
                    amountStr = "- " + amountStr;
                }
                Label amount = new Label(amountStr);
                String amountColor = switch (entry.kind) {
                    case DEPOSIT  -> "#4ade80";
                    case WITHDRAW -> "#ef4444";
                    case BID      -> "WON".equals(entry.status) ? "#4ade80" : "#f0b429";
                };
                amount.setStyle("-fx-text-fill: " + amountColor + "; -fx-font-size: 13px;" +
                                " -fx-font-weight: bold; -fx-font-family: 'Arial';");

                // Timestamp
                Label time = new Label(entry.getFormattedTime());
                time.setStyle("-fx-text-fill: #5a5444; -fx-font-size: 11px;" +
                              " -fx-font-family: 'Arial';");

                Region spacer1 = new Region();
                HBox.setHgrow(spacer1, javafx.scene.layout.Priority.ALWAYS);

                Region spacer2 = new Region();
                spacer2.setMinWidth(24);

                HBox row = new HBox(12, badge, item, spacer1, amount, spacer2, time);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 14 18;");

                setGraphic(row);
                setStyle("-fx-background-color: transparent;");
            }
        });
    }

    // ── Badge style ───────────────────────────────────────────
    private String badgeStyle(String status) {
        String bg, fg;
        switch (status) {
            case "WON"      -> { bg = "rgba(74,222,128,0.15)";  fg = "#4ade80"; }
            case "WINNING"  -> { bg = "rgba(74,222,128,0.15)";  fg = "#4ade80"; }
            case "BID"      -> { bg = "rgba(245,158,11,0.15)";  fg = "#f59e0b"; }
            case "OUTBID"   -> { bg = "rgba(239,68,68,0.15)";   fg = "#ef4444"; }
            case "DEPOSIT"  -> { bg = "rgba(96,165,250,0.15)";  fg = "#60a5fa"; }
            case "WITHDRAW" -> { bg = "rgba(239,68,68,0.10)";   fg = "#f87171"; }
            default         -> { bg = "rgba(245,158,11,0.15)";  fg = "#f59e0b"; }
        }
        return "-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
               " -fx-font-size:10px; -fx-font-weight:bold; -fx-font-family:'Arial';" +
               " -fx-background-radius:20; -fx-padding:3 10; -fx-alignment:center;";
    }

    // ── Navigation ────────────────────────────────────────────
    @FXML
    private void handleNavAuctions() {
        detach();
        NavigationUtils.navigateTo(
                "/com/auction/client/view/AuctionList.fxml", "Live Auctions");
    }

    @FXML
    private void handleNavHistory() {
        // Already on this screen — no-op
    }

    @FXML
    private void handleLogout() {
        detach();
        NavigationUtils.logout();
    }

    // ── Cleanup ───────────────────────────────────────────────
    private void detach() {
        if (transactionListener != null)
            UserSession.getInstance().removeTransactionListener(transactionListener);
    }
}
