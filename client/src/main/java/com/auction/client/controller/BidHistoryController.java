package com.auction.client.controller;

import com.auction.client.sessions.UserSession;
import com.auction.client.util.NavigationUtils;
import com.auction.client.network.ClientMessageSender;
import com.auction.client.network.ServerEventListener;
import com.auction.common.protocol.MessageEnvelope;
import com.auction.common.protocol.MessageType;
import com.auction.common.protocol.ListAuctionsResPayload;
import com.auction.common.protocol.AuctionSummaryItem;
import com.auction.common.protocol.ProtocolMapper;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Consumer;

/**
 * Shows the full transaction log (bids + deposits + withdrawals) from
 * UserSession. The list updates in real-time whenever any screen
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

        // Refresh whenever a new transaction happens
        transactionListener = t -> Platform.runLater(this::loadFromSession);
        UserSession.getInstance().addTransactionListener(transactionListener);
    }

    // ── Load existing transactions ────────────────────────────
    private void loadFromSession() {
        activityListView.getItems().clear();
        
        List<UserSession.Transaction> displayList = new ArrayList<>();
        
        // 1. Add deposit/withdrawal transactions from local session
        for (UserSession.Transaction t : UserSession.getInstance().getTransactions()) {
            if (t.kind != UserSession.Transaction.Kind.BID) {
                displayList.add(t);
            }
        }
        
        // 2. Fetch all auctions from server to reconstruct bid history and results
        com.auction.client.network.ServerConnection connection = com.auction.client.network.ServerConnection.getInstance();
        if (connection.isConnected() && UserSession.getInstance().isLoggedIn()) {
            String currentUsername = UserSession.getInstance().getCurrentUser().getUsername();
            try {
                CompletableFuture<MessageEnvelope> responseFuture = new CompletableFuture<>();
                ClientMessageSender sender = new ClientMessageSender();
                long userId = UserSession.getInstance().getCurrentUser().getId();
                
                String messageId = sender.sendListAuctions(userId, 1, 100, null);
                ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);
                
                MessageEnvelope resEnvelope = responseFuture.get(3, TimeUnit.SECONDS);
                if (resEnvelope.getType() != MessageType.ERROR_RES) {
                    ListAuctionsResPayload res = new ProtocolMapper().parsePayload(resEnvelope, ListAuctionsResPayload.class);
                    
                    for (AuctionSummaryItem summary : res.getAuctions()) {
                        List<String> history = summary.getBidHistory();
                        if (history == null || history.isEmpty()) continue;
                        
                        for (int i = 0; i < history.size(); i++) {
                            String entry = history.get(i);
                            String[] parts = entry.split("  →  ");
                            if (parts.length > 1) {
                                String bidderName = parts[0].trim();
                                if (currentUsername != null && currentUsername.equalsIgnoreCase(bidderName)) {
                                    String amtStr = parts[1].replace("$", "").replace(",", "").trim();
                                    double amount = Double.parseDouble(amtStr);
                                    
                                    // Determine bid status/result
                                    String status = "OUTBID";
                                    if (i == 0) { // Highest bid on this auction
                                        if ("FINISHED".equalsIgnoreCase(summary.getStatus()) || "PAID".equalsIgnoreCase(summary.getStatus())) {
                                            status = "WON";
                                        } else {
                                            status = "BID";
                                        }
                                    }
                                    
                                     LocalDateTime bidTime = null;
                                     if (parts.length > 2) {
                                         try {
                                             bidTime = LocalDateTime.parse(parts[2].trim());
                                         } catch (Exception ex) {}
                                     }
                                     if (bidTime == null) {
                                         LocalDateTime endTime = LocalDateTime.ofInstant(summary.getEndTime(), ZoneId.systemDefault());
                                         if (endTime.isAfter(LocalDateTime.now())) {
                                             bidTime = endTime.minusHours(2).minusMinutes(5 + i * 10);
                                         } else {
                                             bidTime = endTime.minusMinutes(5 + i * 10);
                                         }
                                     }
                                     
                                     // Safety check: ensure bidTime is never in the future relative to client local now
                                     if (bidTime.isAfter(LocalDateTime.now())) {
                                         bidTime = LocalDateTime.now().minusMinutes(5 + i * 10);
                                     }
                                     
                                     displayList.add(new UserSession.Transaction(
                                         UserSession.Transaction.Kind.BID,
                                         summary.getItemName(),
                                         amount,
                                         status,
                                         bidTime
                                     ));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading bid history: " + e.getMessage());
            }
        }
        
        // 3. Sort by time newest first
        displayList.sort((a, b) -> b.time.compareTo(a.time));
        
        activityListView.getItems().addAll(displayList);
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
                String username = UserSession.getInstance().isLoggedIn()
                        ? UserSession.getInstance().getCurrentUser().getUsername()
                        : "You";

                String displayName;
                if (entry.kind == UserSession.Transaction.Kind.BID) {
                    String action = "WON".equals(entry.status) ? "won" : "bid";
                    displayName = String.format("%s %s %,.0f$ %s", 
                            username, action, Math.abs(entry.amount), entry.itemName);
                } else {
                    displayName = switch (entry.kind) {
                        case DEPOSIT  -> "Deposit";
                        case WITHDRAW -> "Withdrawal";
                        default       -> "";
                    };
                }
                Label item = new Label(displayName);
                item.setStyle("-fx-text-fill: #f5f0e6; -fx-font-size: 13px;" +
                              " -fx-font-weight: bold; -fx-font-family: 'Arial';");
                item.setMaxWidth(400);

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
