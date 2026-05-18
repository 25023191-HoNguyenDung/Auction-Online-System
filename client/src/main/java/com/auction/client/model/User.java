package com.auction.client.model;

/**
 * Client-side User — khớp với server DAO:
 * DB columns: id, user_name, email, password, role
 */
public class User {

    private long   id;
    private String username;   // user_name trong DB
    private String fullName;   // hiển thị UI (= username nếu không có)
    private String email;
    private String password;
    private String role;       // ADMIN | SELLER | BIDDER
    private double accountBalance = 50_000.0;

    public User() {}
    public User(long id, String username, String email, String role) {
        this.id       = id;
        this.username = username;
        this.fullName = username;
        this.email    = email;
        this.role     = role;
        this.accountBalance = 0.0;
    }

    public User(long id, String username, String email,
                String password, String role) {
        this(id, username, email, role);
        this.password = password;
    }

    public User(long id, String username, String email,
                String password, String role, double accountBalance) {
        this(id, username, email, password, role);
        this.accountBalance = accountBalance;
    }

    // ── Getters ──────────────────────────────────────────────
    public long   getId()       { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName != null ? fullName : username; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }
    public double getAccountBalance() { return accountBalance; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(long id)           { this.id = id; }
    public void setUsername(String v)    { this.username = v; }
    public void setFullName(String v)    { this.fullName = v; }
    public void setEmail(String v)       { this.email = v; }
    public void setPassword(String v)    { this.password = v; }
    public void setRole(String v)        { this.role = v; }
    public void setAccountBalance(double v) { this.accountBalance = v; }

    // ── Role helpers ──────────────────────────────────────────
    public boolean isAdmin()  { return "ADMIN".equalsIgnoreCase(role); }
    public boolean isSeller() { return "SELLER".equalsIgnoreCase(role); }
    public boolean isBidder() { return "BIDDER".equalsIgnoreCase(role); }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role='" + role + "', balance=" + accountBalance + "}";
    }
}
