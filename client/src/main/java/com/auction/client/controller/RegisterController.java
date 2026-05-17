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

        // TODO: replace with server registration call
        System.out.println("✅ Register | Role: " + role + " | Email: " + email);
        showMessage("Account created successfully as " + role + "!", true);

        // Navigate to login after 1.5s
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            Platform.runLater(this::goToLogin);
        }).start();
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