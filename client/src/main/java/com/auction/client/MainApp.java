package com.auction.client;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        try {
            com.auction.client.network.ServerConnection.getInstance().connect("26.134.195.52", 1337);
            com.auction.client.network.ServerEventListener listener = new com.auction.client.network.ServerEventListener();
            listener.start();
            System.out.println("Connected to localhost:1337!");
        } catch (Exception e) {
            System.err.println("Cannot connect to server: " + e.getMessage() + ". Please start the application first!");
        }
        
        URL fxmlUrl = getClass().getResource(
            "/com/auction/client/view/Login.fxml"
        );
        
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        
        Scene scene = new Scene(loader.load(), 980, 802);
        URL cssUrl = getClass().getResource(
            "/com/auction/client/css/style.css"
        );
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Warning: style.css not found");
        }
        
        stage.setTitle("Auction Online System");
        stage.setResizable(true);
        stage.setMinWidth(996);
        stage.setMinHeight(842);
        
        scene.getRoot().setOpacity(0.0);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();

        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(500), scene.getRoot());
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}