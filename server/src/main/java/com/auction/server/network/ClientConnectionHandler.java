package com.auction.server.network;

import com.auction.common.protocol.MessageEnvelope;
import com.auction.common.protocol.ProtocolMapper;
import com.auction.common.protocol.ProtocolMappingException;

import java.io.BufferedReader;
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
    private volatile  PrintWriter out;
    private final Object writeLock = new Object();

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
        System.out.println("Client connected: " + clientId);
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
                    System.err.println("JSON is invalid from " + clientId + ": " + e.getMessage());
                    sendRawError("INVALID_MESSAGE: " + e.getMessage()); // gửi lỗi về client
                }
            }
        } catch (Exception e) {
            System.err.println("Error occurred while handling client connection " + clientId + ": " + e.getMessage());
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
        System.out.println("Client disconnected: " + clientId);
    }
    // gửi JSON lỗi về client
    private void sendRawError(String message) {
        if (out != null) {
            synchronized (writeLock) {
                out.println("{\"error\":\"" + message + "\"}");
                out.flush();
            }
        }
    }

    public String getClientId() { return clientId; }
}
