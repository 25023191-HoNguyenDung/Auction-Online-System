package com.auction.client.viewmodel;

import com.auction.client.network.ClientMessageSender;
import com.auction.client.network.ServerConnection;
import com.auction.common.protocol.ErrorPayload;
import com.auction.common.protocol.MessageEnvelope;
import com.auction.common.protocol.MessageType;
import com.auction.common.protocol.ProtocolMapper;
import com.auction.common.protocol.RegisterReqPayload;
import com.auction.common.protocol.RegisterResPayload;

public class RegisterViewModel {

    public enum RegisterResult { SUCCESS, EMPTY_FIELDS, PASSWORD_MISMATCH, SERVER_ERROR }

    private String errorMessage = "";
    private final ClientMessageSender sender = new ClientMessageSender();
    private final ProtocolMapper mapper = new ProtocolMapper();

    public RegisterResult register(String username, String email, String password, String confirm, String role) {
        // 1. Validate
        if (username.isBlank() || email.isBlank() || password.isBlank() || confirm.isBlank() || role == null) {
            errorMessage = "Please fill in all fields.";
            return RegisterResult.EMPTY_FIELDS;
        }
        if (!password.equals(confirm)) {
            errorMessage = "Passwords do not match.";
            return RegisterResult.PASSWORD_MISMATCH;
        }
        if (!ServerConnection.getInstance().isConnected()) {
            errorMessage = "Chưa kết nối đến server.";
            return RegisterResult.SERVER_ERROR;
        }
        try {
            // 2. Gửi request lên server
            sender.sendRegister(username, email, password, role);

            // 3. Đọc response
            String responseJson = ServerConnection.getInstance().getIn().readLine();
            MessageEnvelope response = mapper.parseEnvelope(responseJson);

            if (response.getType() == MessageType.REGISTER_RES) {
                // Reset connection để login không bị lệch response
                ServerConnection.getInstance().disconnect();
                try {
                    ServerConnection.getInstance().connect("localhost", 1337);
                } catch (Exception ex) {
                    // ignore
                }
                errorMessage = "";
                return RegisterResult.SUCCESS;

            } else if (response.getType() == MessageType.ERROR_RES) {
                ErrorPayload error = mapper.parsePayload(response, ErrorPayload.class);
                errorMessage = error.getMessage();
                return RegisterResult.SERVER_ERROR;

            } else {
                errorMessage = "Phản hồi không hợp lệ.";
                return RegisterResult.SERVER_ERROR;
            }

        } catch (Exception e) {
            errorMessage = "Lỗi kết nối server: " + e.getMessage();
            return RegisterResult.SERVER_ERROR;
        }
    }

    public String getErrorMessage() { return errorMessage; }
}
