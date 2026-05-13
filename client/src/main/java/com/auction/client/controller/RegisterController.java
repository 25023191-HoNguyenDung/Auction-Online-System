package com.auction.client.controller;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private ComboBox<String> roleComboBox;
    @FXML private TextField userNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox termsCheckBox;
    @FXML private Label errorLabel;
    @FXML private Label loginLabel;
    @FXML private Label goLoginNav;
    @FXML private Button goRegisterNav;
    @FXML private Button registerButton;

    @FXML
    public void initialize() {
        setupNavigation();
        setupRoleComboBox();
    }

    private void setupNavigation() {
        if (goLoginNav != null) goLoginNav.setOnMouseClicked(e -> goToLogin());
        if (loginLabel != null) loginLabel.setOnMouseClicked(e -> goToLogin());
        if (goRegisterNav != null) {
            goRegisterNav.setStyle("-fx-background-color: #d4981f;");
        }
    }

    private void setupRoleComboBox() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("BIDDER", "SELLER");
            roleComboBox.getSelectionModel().select("BIDDER"); // Mặc định Bidder
        }
    }

    @FXML
    private void handleRegister() {
        String role = roleComboBox.getValue();
        String userName = userNameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        errorLabel.setVisible(false);

        if (role == null || userName.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
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

        // TODO: Sau này kết nối với Database / Service
        System.out.println("✅ Đăng ký thành công | Role: " + role + " | Email: " + email);

        showMessage("Account created successfully as " + role + "!", true);

        // Chuyển sang Login sau 1.5 giây
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            Platform.runLater(this::goToLogin);
        }).start();
    }

    private void showMessage(String message, boolean isSuccess) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setStyle(isSuccess ? 
                "-fx-text-fill: #4ade80; -fx-font-size: 13px;" : 
                "-fx-text-fill: #e05252; -fx-font-size: 13px;");
    }
 
    private void goToLogin() {
        navigateTo("/com/auction/client/view/Login.fxml", "Login - Auction Pro");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/com/auction/client/css/style.css").toExternalForm()
            );

            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}