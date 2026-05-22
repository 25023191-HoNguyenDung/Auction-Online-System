package com.auction.client.viewmodel;

import com.auction.client.network.ClientMessageSender;
import com.auction.client.network.ServerConnection;
import com.auction.client.sessions.UserSession;
import com.auction.common.protocol.ErrorPayload;
import com.auction.common.protocol.LoginResPayload;
import com.auction.common.protocol.MessageEnvelope;
import com.auction.common.protocol.MessageType;
import com.auction.common.protocol.ProtocolMapper;
import com.auction.client.model.User;
import com.auction.client.network.ServerEventListener;

public class LoginViewModel {

    public enum LoginResult { SUCCESS, EMPTY_FIELDS, INVALID_CREDENTIALS, SERVER_ERROR }

    private String errorMessage = "";
    private final ClientMessageSender sender = new ClientMessageSender();
    private final ProtocolMapper mapper = new ProtocolMapper();

    public LoginResult login(String username, String password) {
        // 1. Validate
        if (username == null || username.isBlank() ||
                password == null || password.isBlank()) {
            errorMessage = "Please fill in all fields.";
            return LoginResult.EMPTY_FIELDS;
        }

        // 2. Kiểm tra kết nối
        if (!ServerConnection.getInstance().isConnected()) {
            errorMessage = "Chưa kết nối đến server.";
            return LoginResult.SERVER_ERROR;
        }

        try {
            // Dùng CompletableFuture để chờ response
            java.util.concurrent.CompletableFuture<MessageEnvelope> future =
                    new java.util.concurrent.CompletableFuture<>();

            // Gửi request
            String messageId = sender.sendLogin(username.trim(), password);

            // Đăng ký callback theo messageId
            ServerEventListener.getInstance().onResponse(messageId, future::complete);

            // Chờ response tối đa 5 giây
            MessageEnvelope response = future.get(5, java.util.concurrent.TimeUnit.SECONDS);

            if (response.getType() == MessageType.LOGIN_RES) {
                LoginResPayload payload = mapper.parsePayload(response, LoginResPayload.class);
                User user = new User(payload.getUserId(), payload.getUsername(), "", payload.getRole());
                UserSession.getInstance().login(user);
                errorMessage = "";
                return LoginResult.SUCCESS;

            } else if (response.getType() == MessageType.ERROR_RES) {
                ErrorPayload error = mapper.parsePayload(response, ErrorPayload.class);
                errorMessage = error.getMessage();
                return LoginResult.INVALID_CREDENTIALS;

            } else {
                errorMessage = "Phản hồi không hợp lệ từ server.";
                return LoginResult.SERVER_ERROR;
            }

        } catch (java.util.concurrent.TimeoutException e) {
            errorMessage = "Server không phản hồi.";
            return LoginResult.SERVER_ERROR;

        } catch (Exception e) {
            errorMessage = "Lỗi kết nối server: " + e.getMessage();
            return LoginResult.SERVER_ERROR;
        }
    }

    public String getErrorMessage() { return errorMessage; }
}