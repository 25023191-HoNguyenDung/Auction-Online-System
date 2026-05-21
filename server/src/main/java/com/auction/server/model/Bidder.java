package com.auction.server.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Bidder extends User {

    private BigDecimal account_balance;
    private ArrayList<Auction> history_of_auction;

    private final Map<Long, BigDecimal> holdMap = new HashMap<>();

    public Bidder(String name, long ID, String email,
                  String password, String role,
                  BigDecimal account_balance,
                  ArrayList<Auction> history_of_auction) {
        super(name, ID, email, password, role);
        this.account_balance = account_balance;
        this.history_of_auction = history_of_auction;
    }

    @Override
    public void set_role() { this.role = "BIDDER"; }

    public BigDecimal getTotalHeld() {
        return holdMap.values().stream()
            .reduce( BigDecimal.ZERO, (a, b) -> a.add(b) );
    }

    public BigDecimal getAvailableBalance() {
        return account_balance.subtract(getTotalHeld());
    }

    public void holdAmount(long auctionId, BigDecimal amount) {
        if ( amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 )
            throw new IllegalArgumentException("Invalid hold amount");
        holdMap.put(auctionId, amount); // replace nếu đã tồn tại
    }

    public void releaseHold(long auctionId) {
        holdMap.remove(auctionId);
    }

    public boolean canAfford(long auctionId, BigDecimal amount) {
        BigDecimal currentHoldForThisAuction = holdMap.getOrDefault(auctionId, BigDecimal.ZERO);

        // Tiền đang hold ở các phiên KHÁC
        BigDecimal heldElsewhere = getTotalHeld().subtract(currentHoldForThisAuction);

        // Tiền thực sự còn lại 
        BigDecimal available = account_balance.subtract(heldElsewhere);

        return available.compareTo(amount) >= 0;
    }

    public BigDecimal getAccount_balance() { return account_balance; }

    public void setAccount_balance(BigDecimal account_balance) {
        this.account_balance = account_balance;
    }

    /** Trừ hẳn tiền sau khi phiên kết thúc và thanh toán */
    public void deductBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Invalid deduction amount");
        if (amount.compareTo(account_balance) > 0)
            throw new IllegalStateException("Insufficient balance for deduction");
        this.account_balance = account_balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        throw new IllegalArgumentException("Deposit amount must be > 0");
    this.account_balance = account_balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Withdraw amount must be > 0");
        if (amount.compareTo(getAvailableBalance()) > 0)
            throw new IllegalStateException(
                "Cannot withdraw " + amount +
                " — available: " + getAvailableBalance() +
                " (holding " + getTotalHeld() + " in active auctions)"
            );
        this.account_balance = account_balance.subtract(amount);
    }

    public ArrayList<Auction> getHistory_of_auction() { return history_of_auction; }

    public void addAuctionHistory(Auction auction) {
        if (auction != null) history_of_auction.add(auction);
    }
}