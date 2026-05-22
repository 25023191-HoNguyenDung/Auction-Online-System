package com.auction.client;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // ── TỰ ĐỘNG KẾT NỐI TỚI SERVER CỔNG 1337 KHI MỞ CLIENT ─────────────────
        try {
            com.auction.client.network.ServerConnection.getInstance().connect("localhost", 1337);
            com.auction.client.network.ServerEventListener listener = new com.auction.client.network.ServerEventListener();
            listener.start();
            System.out.println(">>> Đã kết nối và lắng nghe server thành công tại localhost:1337!");
        } catch (Exception e) {
            System.err.println(">>> Không thể kết nối tới Server: " + e.getMessage() + ". Vui lòng bật ServerApplication trước!");
        }
        // ─────────────────────────────────────────────────────────────────────
        
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
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}