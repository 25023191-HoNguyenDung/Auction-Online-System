package com.auction.client.controller;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label forgotPasswordLabel;
    @FXML private Label signUpLabel;
    @FXML private Button loginButton;
    @FXML private Button goRegisterNav;   
    @FXML private Button goLoginNav;      

    public void initialize() {
        setupLoginButtonHover();
        setupNavigation();  // thêm

        forgotPasswordLabel.setOnMouseClicked(e -> handleForgotPassword());
        signUpLabel.setOnMouseClicked(e -> handleSignUp());
    }

    private void setupNavigation() {
        // REGISTER ở navbar → sang trang register
        if (goRegisterNav != null) {
            goRegisterNav.setOnMouseClicked(e -> handleSignUp());
        }

        // LOGIN ở navbar → đang ở đây rồi, không cần chuyển
        if (goLoginNav != null) {
            goLoginNav.setOnMouseClicked(e -> System.out.println("Login page now"));
            // Highlight vì đang ở trang login
            goLoginNav.setStyle("-fx-background-color: #d4981f; -fx-text-fill: #0d0c08;");
        }
    }
    private void setupLoginButtonHover() {
        if (loginButton == null) return;

        String originalStyle = loginButton.getStyle();

        loginButton.setOnMouseEntered(e -> {
            loginButton.setStyle(originalStyle.replace("#f0b429", "#d4981f"));
        });

        loginButton.setOnMouseExited(e -> {
            loginButton.setStyle(originalStyle.replace("#d4981f", "#f0b429"));
        });
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        if (email.equals("collector@aureate.com") && password.equals("password")) {
            errorLabel.setVisible(false);
            System.out.println("Login successful!");
            navigateTo("/com/auction/client/view/Main.fxml", "Auction Dashboard");
        } else {
            showError("Invalid email or password.");
        }
    }

    @FXML
    private void handleForgotPassword() {
        navigateTo("/com/auction/client/view/ForgotPassword.fxml", "Forgot Password");
    }

    @FXML
    private void handleSignUp() {
        navigateTo("/com/auction/client/view/Register.fxml", "Register Account");
    }

    // ===================== METHOD CHUYỂN MÀN HÌNH =====================
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/com/auction/client/css/style.css").toExternalForm()
        );

            Stage stage = (Stage) loginButton.getScene().getWindow(); // Lấy stage hiện tại
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể chuyển sang trang: " + title);
        } catch (NullPointerException e) {
            e.printStackTrace();
            showError("Lỗi: Không tìm thấy file FXML");
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else {
            System.err.println("Error: " + message);
        }
    }
}