package com.auction.client.controller;

import com.auction.client.util.NavigationUtils;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private ComboBox<String>  roleComboBox;
    @FXML private TextField         userNameField;
    @FXML private TextField         emailField;
    @FXML private PasswordField     passwordField;
    @FXML private PasswordField     confirmPasswordField;
    @FXML private CheckBox          termsCheckBox;
    @FXML private Label             errorLabel;
    @FXML private Label             loginLabel;
    @FXML private Label             goLoginNav;
    @FXML private Button            goRegisterNav;

    @FXML
    public void initialize() {
        setupNavigation();
        setupRoleComboBox();
    }

    private void setupNavigation() {
        if (goLoginNav    != null) goLoginNav.setOnMouseClicked(e -> goToLogin());
        if (loginLabel    != null) loginLabel.setOnMouseClicked(e -> goToLogin());
        if (goRegisterNav != null) goRegisterNav.setStyle("-fx-background-color: #d4981f;");
    }

    private void setupRoleComboBox() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("BIDDER", "SELLER");
            roleComboBox.getSelectionModel().selectFirst();
        }
    }

   @FXML
    private void handleRegister() {
        String role    = roleComboBox.getValue();
        String userName= userNameField.getText().trim();
        String email   = emailField.getText().trim();
        String pass    = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        errorLabel.setVisible(false);

        // Validate phía client
        if (role == null || userName.isEmpty() || email.isEmpty()
                || pass.isEmpty() || confirm.isEmpty()) {
            showMessage("Please fill in all fields.", false);
            return;
        }
        if (!pass.equals(confirm)) {
            showMessage("Passwords do not match.", false);
            return;
        }
        if (!termsCheckBox.isSelected()) {
            showMessage("You must agree to the Terms of Service.", false);
            return;
        }

        // Kiểm tra kết nối server
        com.auction.client.network.ServerConnection conn =
            com.auction.client.network.ServerConnection.getInstance();
        if (!conn.isConnected()) {
            showMessage("No server connection. Please start the server first!", false);
            return;
        }

        // Gửi request lên server
        try {
            java.util.concurrent.CompletableFuture<com.auction.common.protocol.MessageEnvelope> future =
                new java.util.concurrent.CompletableFuture<>();

            com.auction.client.network.ClientMessageSender sender =
                new com.auction.client.network.ClientMessageSender();

            String messageId = sender.sendRegister(userName, email, pass, role);

            com.auction.client.network.ServerEventListener.getActiveInstance()
                .onResponse(messageId, future::complete);

            // Đợi tối đa 5 giây (chạy trên background thread để không block UI)
            new Thread(() -> {
                try {
                    com.auction.common.protocol.MessageEnvelope res =
                        future.get(5, java.util.concurrent.TimeUnit.SECONDS);

                    Platform.runLater(() -> {
                        if (res.getType() == com.auction.common.protocol.MessageType.ERROR_RES) {
                            com.auction.common.protocol.ErrorPayload err =
                                new com.auction.common.protocol.ProtocolMapper()
                                    .parsePayload(res, com.auction.common.protocol.ErrorPayload.class);
                            showMessage(err.getMessage(), false);
                        } else {
                            showMessage("Account created successfully as " + role + "!", true);
                            // Chuyển về Login sau 1.5s
                            new Thread(() -> {
                                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                                Platform.runLater(this::goToLogin);
                            }).start();
                        }
                    });

                } catch (java.util.concurrent.TimeoutException e) {
                    Platform.runLater(() -> showMessage("Server timeout. Please try again.", false));
                } catch (Exception e) {
                    Platform.runLater(() -> showMessage("Error: " + e.getMessage(), false));
                }
            }).start();

        } catch (Exception e) {
            showMessage("Failed to send request: " + e.getMessage(), false);
        }
    }

    private void goToLogin() {
        NavigationUtils.navigateTo(
            "/com/auction/client/view/Login.fxml", "Login");
    }

    private void showMessage(String message, boolean success) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle(success
            ? "-fx-text-fill: #4ade80; -fx-font-size: 13px;"
            : "-fx-text-fill: #e05252; -fx-font-size: 13px;");
    }
}