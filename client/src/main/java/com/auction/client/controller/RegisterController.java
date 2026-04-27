package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnRegister;
    @FXML private Label lblError;

    @FXML
    public void initialize() {
        lblError.setText("");
    }

    @FXML
    private void handleRegister() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String confirm = txtConfirmPassword.getText().trim();

        // Validate rỗng
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            lblError.setText("Please enter the required infomation!");
            return;
        }

        // Validate username tối thiểu 3 ký tự
        if (username.length() < 3) {
            lblError.setText("Usernames must have at least 3 characters!");
            return;
        }

        // Validate password tối thiểu 6 ký tự
        if (password.length() < 6) {
            lblError.setText("Password must have at least 6 characters!");
            return;
        }

        // Validate confirm password
        if (!password.equals(confirm)) {
            lblError.setText("Verification password does not match!");
            txtConfirmPassword.clear();
            return;
        }

        // TODO: gọi API đăng ký sau
        // Hiện tại mock thành công → chuyển về Login
        lblError.setStyle("-fx-text-fill: #4caf50;");
        lblError.setText("Registration successful! Redirecting...");

        // Chuyển về Login sau 1.5 giây
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(() -> goToLogin());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void goToLogin() {
        switchScene("/com/auction/client/view/Login.fxml");
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(fxmlPath)
            );
            Stage stage = (Stage) btnRegister.getScene().getWindow();
            Scene scene = new Scene(loader.load(), 1024, 768);
            scene.getStylesheets().add(
                getClass().getResource("/com/auction/client/css/style.css")
                          .toExternalForm()
            );
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}