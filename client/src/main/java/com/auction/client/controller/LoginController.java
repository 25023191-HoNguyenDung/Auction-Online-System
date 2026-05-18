package com.auction.client.controller;

import com.auction.client.util.NavigationUtils;
import com.auction.client.viewmodel.LoginViewModel;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Label         forgotPasswordLabel;
    @FXML private Label         signUpLabel;
    @FXML private Button        loginButton;
    @FXML private Button        goRegisterNav;
    @FXML private Button        goLoginNav;

    private final LoginViewModel viewModel = new LoginViewModel();

    @FXML
    public void initialize() {
        setupNavigation();

        forgotPasswordLabel.setOnMouseClicked(e -> handleForgotPassword());
        signUpLabel.setOnMouseClicked(e -> handleSignUp());
    }

    private void setupNavigation() {
        if (goRegisterNav != null)
            goRegisterNav.setOnMouseClicked(e -> handleSignUp());

        if (goLoginNav != null)
            goLoginNav.setStyle("-fx-background-color: #d4981f; -fx-text-fill: #0d0c08;");
    }
    // ── Login ─────────────────────────────────────────────────
    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        hideError();

        LoginViewModel.LoginResult result = viewModel.login(email, password);

        switch (result) {
            case SUCCESS             -> NavigationUtils.navigateToDashboard();
            case EMPTY_FIELDS,
                 INVALID_CREDENTIALS -> showError(viewModel.getErrorMessage());
        }
    }
    // ── Navigation ────────────────────────────────────────────
    @FXML
    private void handleForgotPassword() {
        NavigationUtils.navigateTo(
            "/com/auction/client/view/ForgotPassword.fxml", "Forgot Password");
    }
    @FXML
    private void handleSignUp() {
        NavigationUtils.navigateTo(
            "/com/auction/client/view/Register.fxml", "Register Account");
    }
    // ── UI helpers ────────────────────────────────────────────
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }
    private void hideError() {
        if (errorLabel != null) errorLabel.setVisible(false);
    }
}