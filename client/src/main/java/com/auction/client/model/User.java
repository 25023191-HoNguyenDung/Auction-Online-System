package com.auction.client.model;

public class User {
    private int id;
    private String fullName;
    private String email;
    private String role;        // ADMIN, BIDDER, SELLER

    public User() {}

    public User(int id, String fullName, String email, String role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    // Getters & Setters
    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public void setId(int id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
}