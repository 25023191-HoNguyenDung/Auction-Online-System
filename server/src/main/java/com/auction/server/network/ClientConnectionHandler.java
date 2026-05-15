package com.auction.server.network;

import com.auction.common.protocol.MessageEnvelope;
import com.auction.common.protocol.ProtocolMapper;
import com.auction.common.protocol.ProtocolMappingException;
import com.auction.server.observer.SubscriptionRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
// xly knoi cho từng client
public class ClientConnectionHandler implements Runnable {
    private final Socket socket;
    private final String clientId;
    private final ProtocolMapper mapper;
    private final RequestDispatcher dispatcher; // điều req đến server phù hợp
    private final SubscriptionRegistry subscriptionRegistry; // ngắt knoi client đến phiên
    private PrintWriter out;

    public ClientConnectionHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket;
        this.clientId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        this.mapper = new ProtocolMapper();
        this.dispatcher = dispatcher;
        this.subscriptionRegistry = SubscriptionRegistry.getInstance();
    }

    // xly giao tiếp giữa server và 1 client
    @Override
    public void run() {
        System.out.println("Client kết nối: " + clientId);
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // đọc dlieu client gửi
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true) // gửi dlieu về client
        ) {
            this.out = writer;
            String line;

            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    MessageEnvelope envelope = mapper.parseEnvelope(line); // chuyển JSON -> obj
                    dispatcher.dispatch(envelope, clientId, out); // gửi req đến đúng nơi xly
                } catch (ProtocolMappingException e) {
                    System.err.println("JSON không hợp lệ từ " + clientId + ": " + e.getMessage());
                    sendRawError("INVALID_MESSAGE: " + e.getMessage()); // gửi lỗi về client
                }
            }
        } catch (Exception e) {
            System.err.println(" Lỗi kết nối client " + clientId + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    // dọn khi client ngắt knoi
    private void cleanup() {
        subscriptionRegistry.unsubscribeAll(clientId); // hủy theo dõi
        try {
            if (!socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
        System.out.println(" Client ngắt kết nối: " + clientId);
    }
    // gửi JSON lỗi về client
    private void sendRawError(String message) {
        if (out != null) {
            synchronized (out) {
                out.println("{\"error\":\"" + message + "\"}");
                out.flush();
            }
        }
    }

    public String getClientId() { return clientId; }
}
