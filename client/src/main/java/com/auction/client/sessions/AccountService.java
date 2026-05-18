package com.auction.client.sessions;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.auction.client.model.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AccountService {
    private static final AccountService INSTANCE = new AccountService();
     private static final double DEFAULT_BALANCE = 50_000.0;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<String> bidHistory = FXCollections.observableArrayList();
    private final ObservableList<String> paymentHistory = FXCollections.observableArrayList();
    private final Map<Long, Double> heldBidsByAuction = new HashMap<>();
    private double guestBalance = DEFAULT_BALANCE;

    private AccountService() {}

    public static AccountService getInstance() {
        return INSTANCE;
    }

    public double getBalance() {
    
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) return guestBalance;
        return user.getAccountBalance();
    }


    public void deposit(double amount) {
        changeBalance(amount);
        addPayment("Deposit", amount);
    }

    public boolean withdraw(double amount) {
        if (amount <= 0 || amount > getBalance()) return false;
        changeBalance(-amount);
        addPayment("Withdraw", -amount);
        return true;
    }

    public boolean placeBid(long auctionId, String itemName, double amount) {
        double previousHold = heldBidsByAuction.getOrDefault(auctionId, 0.0);
        double required = amount - previousHold;
        if (required <= 0 || required > getBalance()) return false;

        if (previousHold > 0) {
            changeBalance(previousHold);
            addPayment("Refund previous bid on " + itemName, previousHold);
        }

        changeBalance(-amount);
        heldBidsByAuction.put(auctionId, amount);
        bidHistory.add(0, stamp() + "  " + itemName + "  bid  " + fmt(amount));

        addPayment("Bid hold on " + itemName, -amount);
        return true;
    }

    public ObservableList<String> getBidHistory() {
        return bidHistory;
    }

    public ObservableList<String> getPaymentHistory() {
        return paymentHistory;
    }

     private void changeBalance(double delta) {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) {
             guestBalance += delta;
            return;
        }
        user.setAccountBalance(getBalance() + delta);
    }

    private void addPayment(String label, double amount) {
        paymentHistory.add(0, stamp() + "  " + label + "  " + fmtSigned(amount));
    }

    private String stamp() {
        return LocalTime.now().format(TIME_FORMAT);
    }

    private String fmt(double value) {
        return String.format("$%,.0f", value);
    }

    private String fmtSigned(double value) {
        return String.format("%s$%,.0f", value >= 0 ? "+" : "-", Math.abs(value));
    }
}
