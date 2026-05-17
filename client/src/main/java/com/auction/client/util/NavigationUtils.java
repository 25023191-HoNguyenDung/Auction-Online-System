package com.auction.client.util;

import java.io.IOException;

import com.auction.client.controller.AuctionDetailController;
import com.auction.client.controller.BidController;
import com.auction.client.model.AuctionItem;
import com.auction.client.sessions.UserSession;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationUtils {

    public static void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationUtils.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                NavigationUtils.class.getResource("/com/auction/client/css/style.css").toExternalForm()
            );

            Stage stage = getCurrentStage();
            if (stage != null) {
                stage.setTitle(title + " — Auction Pro");
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Navigation failed: " + fxmlPath);
        }
    }
    /**
     * Navigate to AuctionDetail and pass the selected item to its controller.
     */
    public static void navigateToAuctionDetail(AuctionItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                NavigationUtils.class.getResource("/com/auction/client/view/AuctionDetail.fxml")
            );
            Parent root = loader.load();

            // Get the controller and inject the item AFTER load
            AuctionDetailController controller = loader.getController();
            controller.setAuctionItem(item);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                NavigationUtils.class.getResource("/com/auction/client/css/style.css").toExternalForm()
            );

            Stage stage = getCurrentStage();
            if (stage != null) {
                stage.setTitle(item.getItemName() + " — Auction Pro");
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Navigation to AuctionDetail failed");
        }
    }

    /**
     * Navigate to BidScreen and pass the selected item to its controller.
     */
    public static void navigateToBidScreen(AuctionItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                NavigationUtils.class.getResource("/com/auction/client/view/BidScreen.fxml")
            );
            Parent root = loader.load();

            BidController controller = loader.getController();
            controller.setAuctionItem(item);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                NavigationUtils.class.getResource("/com/auction/client/css/style.css").toExternalForm()
            );

            Stage stage = getCurrentStage();
            if (stage != null) {
                stage.setTitle("Place Bid — " + item.getItemName() + " — Auction Pro");
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Navigation to BidScreen failed");
        }
    }

    private static Stage getCurrentStage() {
        for (var window : Stage.getWindows()) {
            if (window instanceof Stage s && s.isShowing()) {
                return s;
            }
        }
        return null;
    }

    public static void navigateToDashboard() {
        UserSession session = UserSession.getInstance();
        
        if (!session.isLoggedIn()) {
            navigateTo("/com/auction/client/view/Login.fxml", "Login");
            return;
        }

        String role = session.getCurrentUser().getRole().toUpperCase();
        switch (role) {
            case "ADMIN"  -> navigateTo("/com/auction/client/view/AdminDashboard.fxml", "Admin Dashboard");
            case "SELLER" -> navigateTo("/com/auction/client/view/SellerDashboard.fxml", "Seller Dashboard");
            default       -> navigateTo("/com/auction/client/view/AuctionList.fxml", "Live Auctions");
        }
    }

    public static void logout() {
        UserSession.getInstance().logout();
        navigateTo("/com/auction/client/view/Login.fxml", "Login");
    }
}