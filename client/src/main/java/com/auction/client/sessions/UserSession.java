package com.auction.client.sessions;

import com.auction.client.model.User;   // sẽ tạo sau

public class UserSession {

    private static UserSession instance;
    private User currentUser;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
        System.out.println("Đăng nhập thành công: " + user.getFullName() + " (" + user.getRole() + ")");
    }

    public void logout() {
        this.currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isSeller() {
        return currentUser != null && "SELLER".equalsIgnoreCase(currentUser.getRole());
    }

    public boolean isBidder() {
        return currentUser != null && "BIDDER".equalsIgnoreCase(currentUser.getRole());
    }
}