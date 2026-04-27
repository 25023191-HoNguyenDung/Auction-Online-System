package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblError;
    @FXML
    public void initialize() {
        lblError.setText("");
        txtPassword.setOnAction(e -> handleLogin());
    }
    @FXML
    private void handleLogin(){
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Please enter the required infomation!");
            return;
        }

        if (username.equals("admin") && password.equals("123")) {
            switchScene("/com/auction/client/view/AuctionList.fxml");
        } else {
            lblError.setText("Incorrect username or password!");
            txtPassword.clear();
        }
    }
    @FXML
    private void goToRegister(){
        switchScene("/com/auction/client/view/Register.fxml");
    }
    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(fxmlPath)
            );
            Stage stage = (Stage) btnLogin.getScene().getWindow();
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
