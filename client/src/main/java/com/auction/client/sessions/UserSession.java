package com.auction.client.sessions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.auction.client.model.User;

/**
 * Singleton session — holds the logged-in user, their balance,
 * and a transaction log that is shared across every screen.
 *
 * Screens subscribe via addBalanceListener() and are notified
 * immediately whenever the balance or transaction list changes.
 */
public class UserSession {

    // ── Singleton ──────────────────────────────────────────────
    private static UserSession instance;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

        // ── Session state ─────────────────────────────────────────
    private User    currentUser;
    private double  balance = 50_000.0;   // Đây là Tổng số dư (Total Balance)
    private final java.util.Map<String, Double> holdMap = new java.util.HashMap<>(); // holdMap giữ tiền theo phiên

    // ── Transaction log (shared with BidHistory) ──────────────
    public static class Transaction {
        public enum Kind { DEPOSIT, WITHDRAW, BID }

        public final Kind          kind;
        public final String        itemName;   // auction name (for BID), or "" for deposit/withdraw
        public final double        amount;
        public final String        status;     // "DEPOSIT" | "WITHDRAW" | "WINNING" | "OUTBID"
        public final LocalDateTime time;

        private static final DateTimeFormatter FMT =
                DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");

        public Transaction(Kind kind, String itemName, double amount,
                           String status, LocalDateTime time) {
            this.kind     = kind;
            this.itemName = itemName;
            this.amount   = amount;
            this.status   = status;
            this.time     = time;
        }

        public String getFormattedTime() { return time.format(FMT); }
    }

    private final List<Transaction> transactions = new ArrayList<>();

    // ── Listeners ─────────────────────────────────────────────
    /** Called on JavaFX Application Thread whenever balance changes. */
    private final List<Consumer<Double>> balanceListeners = new ArrayList<>();

    /** Called on JavaFX Application Thread whenever a new transaction is added. */
    private final List<Consumer<Transaction>> transactionListeners = new ArrayList<>();

    public void addBalanceListener(Consumer<Double> listener) {
        balanceListeners.add(listener);
    }

    public void removeBalanceListener(Consumer<Double> listener) {
        balanceListeners.remove(listener);
    }

    public void addTransactionListener(Consumer<Transaction> listener) {
        transactionListeners.add(listener);
    }

    public void removeTransactionListener(Consumer<Transaction> listener) {
        transactionListeners.remove(listener);
    }

    private void notifyBalance() {
        double snap = balance;
        for (Consumer<Double> l : balanceListeners) {
            try { l.accept(snap); } catch (Exception ignored) {}
        }
    }

    private void notifyTransaction(Transaction t) {
        for (Consumer<Transaction> l : transactionListeners) {
            try { l.accept(t); } catch (Exception ignored) {}
        }
    }

        // ── Balance operations ────────────────────────────────────
    // Trả về số dư khả dụng để tất cả Label trên UI tự động đồng bộ hiển thị
    public double getBalance() { 
        return getAvailableBalance(); 
    }

    public void setBalance(double newBalance) {
        this.balance = newBalance;
        notifyBalance();
    }
    
    /**
     * Deposit money.  Adds a DEPOSIT entry to the transaction log.
     * @throws IllegalArgumentException if amount <= 0
     */
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be > 0");
        balance += amount;
        Transaction t = new Transaction(
                Transaction.Kind.DEPOSIT, "", amount, "DEPOSIT", LocalDateTime.now());
        transactions.add(0, t);
        notifyBalance();
        notifyTransaction(t);
    }

    /**
     * Withdraw money.  Adds a WITHDRAW entry to the transaction log.
     * @throws IllegalStateException if insufficient funds
     */
    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdraw amount must be > 0");
        if (amount > balance) throw new IllegalStateException("Insufficient balance");
        balance -= amount;
        Transaction t = new Transaction(
                Transaction.Kind.WITHDRAW, "", amount, "WITHDRAW", LocalDateTime.now());
        transactions.add(0, t);
        notifyBalance();
        notifyTransaction(t);
    }

    /**
     * Place a bid - only holds the money and records a "BID" transaction.
     * Status will be updated to WINNING or OUTBID later.
     */
    public boolean placeBid(String itemName, double amount) {
        if (amount <= 0) return false;
        
        double available = getAvailableBalance() + holdMap.getOrDefault(itemName, 0.0);
        if (amount > available) return false;

        // Cập nhật hold (khóa tiền)
        holdMap.put(itemName, amount);

        // Tạo transaction với status = "BID" (không phải WINNING)
        Transaction t = new Transaction(
                Transaction.Kind.BID, 
                itemName, 
                amount,                    // Changed to positive amount
                "BID",                      // ← Sửa ở đây
                LocalDateTime.now());
        
        transactions.add(0, t);
        notifyBalance();
        notifyTransaction(t);
        return true;
    }

    /**
     * Refund/Release the hold of the previous bid when the user is outbid.
     * Changes the matching WINNING transaction to OUTBID and releases the hold.
     */
    public void refundOutbid(String itemName, double refundAmount) {
        // Giải phóng hold cho phiên này
        holdMap.remove(itemName);
        
        // Mark the most recent WINNING bid on this item as OUTBID
        for (Transaction t : transactions) {
            if (t.kind == Transaction.Kind.BID
                    && "WINNING".equals(t.status)
                    && t.itemName.equals(itemName)) {
                int idx = transactions.indexOf(t);
                transactions.set(idx, new Transaction(
                        Transaction.Kind.BID, itemName, refundAmount, "OUTBID", t.time));
                break;
            }
        }
        notifyBalance();
    }

    // Tính tổng tiền đang bị khóa
    public double getTotalHeld() {
        return holdMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    
    // Tính số dư khả dụng (Available Balance) = Tổng tiền - Tiền đang bị khóa
    public double getAvailableBalance() {
        return balance - getTotalHeld();
    }

    // Deducts the final amount from the winner's balance when the auction ends
    public void deductWinnerBalance(double amount) {
        this.balance = Math.max(0, this.balance - amount);
        notifyBalance();
    }

    /** Read-only view of all transactions (newest first). */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }
    // Khi thắng cuộc: giải phóng hold và trừ tiền thật vào Tổng số dư
    public void deductWinnerBalance(String itemName, double amount) {
        holdMap.remove(itemName); // Giải phóng hold
        balance -= amount;        // Trừ trực tiếp vào Tổng số dư
        notifyBalance();
    }
    
    
    // ── Login / logout ────────────────────────────────────────
    public void login(User user) {
        this.currentUser = user;
        this.balance     = user.getBalance();   // Nạp số dư thực tế từ Database
        transactions.clear();
        // Tạo một lịch sử giao dịch ban đầu tương đương với số dư thật
        transactions.add(new Transaction(Transaction.Kind.DEPOSIT, "", user.getBalance(), "DEPOSIT", LocalDateTime.now()));
        System.out.println("Logged in: " + user.getFullName() + " (" + user.getRole() + ") | Balance: " + user.getBalance());
    }

    public void logout() {
        this.currentUser = null;
        balanceListeners.clear();
        transactionListeners.clear();
        transactions.clear();
        holdMap.clear(); // Xóa sạch các khoản hold
        com.auction.client.viewmodel.AuctionListViewModel.clearData();
    }

    private void seedMockTransactions() {
        LocalDateTime now = LocalDateTime.now();
        transactions.add(new Transaction(Transaction.Kind.DEPOSIT,  "",                      50_000, "DEPOSIT",  now.minusDays(1)));
        transactions.add(new Transaction(Transaction.Kind.BID,      "Pioneer Zenith Hybrid", 245_000, "WINNING", now.minusMinutes(3)));
        transactions.add(new Transaction(Transaction.Kind.BID,      "Ethereal Horizon",       18_900, "WINNING", now.minusMinutes(28)));
        transactions.add(new Transaction(Transaction.Kind.BID,      "Vanguard Tourbillon",    82_400, "OUTBID",  now.minusHours(1)));
        transactions.add(new Transaction(Transaction.Kind.BID,      "Neon Phantom",            9_500, "OUTBID",  now.minusHours(2)));
    }

    // ── Getters ───────────────────────────────────────────────
    public User    getCurrentUser() { return currentUser; }
    public boolean isLoggedIn()     { return currentUser != null; }
    public boolean isAdmin()        { return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole()); }
    public boolean isSeller()       { return currentUser != null && "SELLER".equalsIgnoreCase(currentUser.getRole()); }
    public boolean isBidder()       { return currentUser != null && "BIDDER".equalsIgnoreCase(currentUser.getRole()); }
}