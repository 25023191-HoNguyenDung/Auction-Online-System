package com.auction.client.viewmodel;

import com.auction.client.model.User;
import com.auction.client.sessions.UserSession;

/**
 * LoginViewModel
 * Owns all login business logic:
 *  - input validation
 *  - credential checking (mock — replace with server call later)
 *  - session management
 *
 * Controller only calls this and reacts to the result.
 */
public class LoginViewModel {

    // ── Result returned to controller ─────────────────────────
    public enum LoginResult {
        SUCCESS,
        EMPTY_FIELDS,
        INVALID_CREDENTIALS
    }

    private String errorMessage = "";

    // ── Main login method ─────────────────────────────────────
    public LoginResult login(String emailOrUsername, String password) {
        // 1. Validate inputs
        if (emailOrUsername == null || emailOrUsername.isBlank() ||
            password == null || password.isBlank()) {
            errorMessage = "Please fill in all fields.";
            return LoginResult.EMPTY_FIELDS;
        }

        // 2. Kiểm tra trạng thái mạng
        com.auction.client.network.ServerConnection connection = com.auction.client.network.ServerConnection.getInstance();
        if (!connection.isConnected()) {
            errorMessage = "No server connection. Please start Server first!";
            return LoginResult.INVALID_CREDENTIALS;
        }

        try {
            // Sử dụng CompletableFuture để đồng bộ hóa tạm thời kết quả trả về bất đồng bộ từ socket
            java.util.concurrent.CompletableFuture<com.auction.common.protocol.MessageEnvelope> responseFuture = new java.util.concurrent.CompletableFuture<>();
            com.auction.client.network.ClientMessageSender sender = new com.auction.client.network.ClientMessageSender();
            
            // Gửi yêu cầu đăng nhập
            String messageId = sender.sendLogin(emailOrUsername.trim(), password);

            // Đăng ký callback chờ server phản hồi
            com.auction.client.network.ServerEventListener.getActiveInstance().onResponse(messageId, responseFuture::complete);

            // Đợi tối đa 5 giây
            com.auction.common.protocol.MessageEnvelope resEnvelope = responseFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);

            // Kiểm tra gói phản hồi là lỗi hay thành công
            if (resEnvelope.getType() == com.auction.common.protocol.MessageType.ERROR_RES) {
                com.auction.common.protocol.ErrorPayload err = new com.auction.common.protocol.ProtocolMapper().parsePayload(resEnvelope, com.auction.common.protocol.ErrorPayload.class);
                errorMessage = err.getMessage();
                return LoginResult.INVALID_CREDENTIALS;
            }

            com.auction.common.protocol.LoginResPayload res = new com.auction.common.protocol.ProtocolMapper().parsePayload(resEnvelope, com.auction.common.protocol.LoginResPayload.class);
            if (res.isSuccess()) {
                // Tạo đối tượng User thật trả về từ DB kèm theo số dư thực tế
                double balance = res.getBalance() != null ? res.getBalance().doubleValue() : 0.0;
                User user = new User(res.getUserId(), res.getUsername(), emailOrUsername, res.getRole(), balance);
                com.auction.client.sessions.UserSession.getInstance().login(user);
                errorMessage = "";
                return LoginResult.SUCCESS;
            } else {
                errorMessage = "Invalid username or password.";
                return LoginResult.INVALID_CREDENTIALS;
            }

        } catch (Exception e) {
            errorMessage = "Connection timeout or error: " + e.getMessage();
            return LoginResult.INVALID_CREDENTIALS;
        }
    }

    // ── Validation helpers ────────────────────────────────────
    public boolean isEmailValid(String email) {
        return email != null && !email.isBlank()
            && email.contains("@") && email.contains(".");
    }

    public boolean isPasswordValid(String password) {
        return password != null && password.length() >= 6;
    }

    // ── Error message ─────────────────────────────────────────
    public String getErrorMessage() {
        return errorMessage;
    }
    // ── Mock credentials ──────────────────────────────────────
    /**
     * Returns a User for known mock credentials, null otherwise.
     * Replace this entire method body with a server call when ready.
     */
    private User resolveMockUser(String email, String password) {
        return switch (email.toLowerCase()) {
            case "admin@auctionpro.com" ->
                password.equals("admin123")
                    ? new User(0L, "Administrator", email, "ADMIN") : null;
            case "collector@aureate.com" ->
                password.equals("password")
                    ? new User(1L, "@collector_a", email, "BIDDER") : null;
            case "seller@aureate.com" ->
                password.equals("seller123")
                    ? new User(2L, "@sterlinghouse", email, "SELLER") : null;
            default -> null;
        };
    }
}