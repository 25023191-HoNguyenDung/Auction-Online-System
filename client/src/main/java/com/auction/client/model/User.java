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

    public User() {}

    public User(long id, String username, String email, String role) {
        this.id       = id;
        this.username = username;
        this.fullName = username;
        this.email    = email;
        this.role     = role;
    }

    public User(long id, String username, String email,
                String password, String role) {
        this(id, username, email, role);
        this.password = password;
    }

    // ── Getters ──────────────────────────────────────────────
    public long   getId()       { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName != null ? fullName : username; }
    public String getEmail()    { return email; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }

    // ── Setters ──────────────────────────────────────────────
    public void setId(long id)           { this.id = id; }
    public void setUsername(String v)    { this.username = v; }
    public void setFullName(String v)    { this.fullName = v; }
    public void setEmail(String v)       { this.email = v; }
    public void setPassword(String v)    { this.password = v; }
    public void setRole(String v)        { this.role = v; }

    // ── Role helpers ──────────────────────────────────────────
    public boolean isAdmin()  { return "ADMIN".equalsIgnoreCase(role); }
    public boolean isSeller() { return "SELLER".equalsIgnoreCase(role); }
    public boolean isBidder() { return "BIDDER".equalsIgnoreCase(role); }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }
}
