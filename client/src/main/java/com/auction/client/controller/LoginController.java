package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label forgotPasswordLabel;
    @FXML private Label signUpLabel;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        // Hover effect cho LOGIN button
        loginButton.setOnMouseEntered(e ->
            loginButton.setStyle(loginButton.getStyle()
                .replace("#f0b429", "#d4981f")));
        loginButton.setOnMouseExited(e ->
            loginButton.setStyle(loginButton.getStyle()
                .replace("#d4981f", "#f0b429")));

        // Click Forgot Password
        forgotPasswordLabel.setOnMouseClicked(e -> handleForgotPassword());

        // Click Sign Up
        signUpLabel.setOnMouseClicked(e -> handleSignUp());
    }

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // --- Validation ---
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        // TODO: Thay bằng logic xác thực thật (DB, API,...)
        if (email.equals("collector@aureate.com") && password.equals("password")) {
            errorLabel.setVisible(false);
            System.out.println("Login successful!");
            // TODO: chuyển sang màn hình chính
            // FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            // Stage stage = (Stage) loginButton.getScene().getWindow();
            // stage.setScene(new Scene(loader.load()));
        } else {
            showError("Invalid email or password.");
        }
    }
    @FXML
    private void handleForgotPassword() {
        System.out.println("Forgot password clicked");
        // TODO: Mở màn hình forgot password
    }
    @FXML
    private void handleSignUp() {
        System.out.println("Sign up clicked");
        // TODO: Mở màn hình đăng ký
    }
    @FXML
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
