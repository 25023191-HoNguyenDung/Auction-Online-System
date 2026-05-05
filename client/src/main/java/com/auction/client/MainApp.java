package com.auction.client;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        
        URL fxmlUrl = getClass().getResource(
            "/com/auction/client/view/Login.fxml"
        );
        
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        
        Scene scene = new Scene(loader.load(), 980, 802);
        URL cssUrl = getClass().getResource(
            "/com/auction/client/css/style.css"
        );
        scene.getStylesheets().add(cssUrl.toExternalForm());
        
        stage.setTitle("Auction Online System");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}