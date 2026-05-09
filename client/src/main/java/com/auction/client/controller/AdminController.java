package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminController {

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome, Administrator");
        }
        System.out.println("Admin Dashboard loaded successfully!");
    }

    // Sau này bạn có thể thêm các chức năng ở đây
    // Ví dụ: quản lý user, quản lý auction, v.v.
}