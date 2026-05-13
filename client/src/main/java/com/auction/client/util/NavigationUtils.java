package com.auction.client.util;

import java.io.IOException;

import com.auction.client.sessions.UserSession;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

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
            } else {
                System.err.println("Không tìm thấy Stage hiện tại!");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Không thể mở trang: " + fxmlPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Stage getCurrentStage() {
        for (Window window : Stage.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                return (Stage) window;
            }
        }
        return null;
    }

    // Các method khác giữ nguyên
    public static void navigateToDashboard() {
        UserSession session = UserSession.getInstance();
        
        if (!session.isLoggedIn()) {
            navigateTo("/com/auction/client/view/Login.fxml", "Login");
            return;
        }

        String role = session.getCurrentUser().getRole().toUpperCase();

        switch (role) {
            case "ADMIN" -> navigateTo("/com/auction/client/view/AdminDashboard.fxml", "Admin Dashboard");
            case "SELLER" -> navigateTo("/com/auction/client/view/SellerDashboard.fxml", "Seller Dashboard");
            case "BIDDER", "USER" -> navigateTo("/com/auction/client/view/AuctionList.fxml", "Bidder Dashboard");
            default -> navigateTo("/com/auction/client/view/AuctionList.fxml", "Dashboard");
        }
    }

    public static void logout() {
        UserSession.getInstance().logout();
        navigateTo("/com/auction/client/view/Login.fxml", "Login");
    }
}