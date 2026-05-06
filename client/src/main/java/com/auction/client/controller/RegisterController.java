package com.auction.client.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private CheckBox termsCheckBox;
    @FXML private Label errorLabel;
    @FXML private Label loginLabel;          // "Login here"
    @FXML private Label goLoginNav;          // Navbar LOGIN
    @FXML private Button goRegisterNav;      // Navbar REGISTER
    @FXML private Button registerButton;     // Nút chính Join the Elite

    @FXML
    public void initialize() {
        setupNavigation();
    }

    private void setupNavigation() {
        // Click LOGIN ở navbar
        if (goLoginNav != null) {
            goLoginNav.setOnMouseClicked(e -> goToLogin());
        }

        // Click REGISTER ở navbar
        if (goRegisterNav != null) {
            goRegisterNav.setOnMouseClicked(e -> goToRegister());  // hiện tại đang ở đây
        }

        // Click "Login here" dưới form
        if (loginLabel != null) {
            loginLabel.setOnMouseClicked(e -> goToLogin());
        }

        // Highlight nút REGISTER vì đang ở trang này
        if (goRegisterNav != null) {
            goRegisterNav.setStyle("-fx-background-color: #d4981f; -fx-text-fill: #0d0c08;");
        }
    }

    private void goToLogin() {
        navigateTo("/com/auction/client/view/Login.fxml", "Login - Auction Pro");
    }

    private void goToRegister() {
        // Đang ở Register rồi thì không cần chuyển
        System.out.println("Register page now");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/com/auction/client/css/style.css").toExternalForm()
            );

            Stage stage = (Stage) goRegisterNav.getScene().getWindow();

            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể chuyển trang");
        }
    }

    @FXML
    private void handleRegister() {
        // code validation...
        showError("Account created successfully!");
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }
}