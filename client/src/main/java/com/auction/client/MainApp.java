package com.auction.client;

import java.io.IOException;
import java.net.URL;

import com.auction.client.network.ServerConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static final String HOST = "localhost";
    private static final int    PORT = 1337;

    @Override
    public void start(Stage stage) throws Exception {

        try {
            ServerConnection.getInstance().connect(HOST, PORT);
        } catch (IOException e) {
            System.err.println("Không thể kết nối server: " + e.getMessage());
        }
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

        public static void main(String[] args) { launch(args);}
}