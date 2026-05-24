package com.auction.common.protocol;

public class UserSummaryItem {
    private long id;
    private String username;
    private String email;
    private String role;
    private double balance;

    public UserSummaryItem() {}

    public UserSummaryItem(long id, String username, String email, String role, double balance) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.balance = balance;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}
