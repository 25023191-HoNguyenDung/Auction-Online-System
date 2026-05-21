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
            // 3. Gửi request lên server
            sender.sendLogin(username.trim(), password);

            // 4. Đọc response từ server
            String responseJson = ServerConnection.getInstance().getIn().readLine();
            System.out.println("SERVER RESPONSE: " + responseJson); // thêm dòng này
            MessageEnvelope response = mapper.parseEnvelope(responseJson);
            // 5. Xử lý response
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

        } catch (Exception e) {
            errorMessage = "Lỗi kết nối server: " + e.getMessage();
            return LoginResult.SERVER_ERROR;
        }
    }

    public String getErrorMessage() { return errorMessage; }
}