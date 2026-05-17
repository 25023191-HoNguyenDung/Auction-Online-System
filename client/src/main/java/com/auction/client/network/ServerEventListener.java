package com.auction.client.network;

import com.auction.common.protocol.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// đọc message từ server và gọi hàm xử lý tương ứng
public class ServerEventListener implements Runnable {
    private final ServerConnection connection; // knoi tới server
    private final ProtocolMapper mapper;
    //lưu callback chờ res
    private final Map<String, Consumer<MessageEnvelope>> callbacks = new ConcurrentHashMap<>();

    private Consumer<BidUpdatedEventPayload> onBidUpdated;
    private Consumer<AuctionClosedEventPayload> onAuctionClosed;
    private Consumer<ErrorPayload> onError;

    private volatile boolean running = false; // check verser có đag chạy không

    public ServerEventListener() {
        this.connection = ServerConnection.getInstance();
        this.mapper = new ProtocolMapper();
    }

    // chạy ServerEventListener
    public void start() {
        running = true;
        Thread thread = new Thread(this, "server_event_listener"); // tạo thread chạy đtg htai
        thread.setDaemon(true); // thread nền
        thread.start();
    }

    // ngừng nghe dlieu từ server
    public void stop() {
        running = false;
    }

    // đọc dlieu -> obj để xử lý
    @Override
    public void run() {
        BufferedReader in = connection.getIn(); // đọc dlieu từ server
        String line;
        try {
            while (running && (line = in.readLine()) != null) {
                if (line.isBlank()) continue; // bỏ qua dòng trống
                try {
                    MessageEnvelope envelope = mapper.parseEnvelope(line); // chuyển sag JSON
                    handleEnvelope(envelope);
                } catch (Exception e) {
                    System.err.println("Lỗi parse message: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) System.err.println(" Mất kết nối với server: " + e.getMessage());
        }
    }

    // nhận mess từ server và chia ra xử lý
    private void handleEnvelope(MessageEnvelope envelope) {
        String correlationId = envelope.getCorrelationId(); // khớp với
        if (correlationId != null && !correlationId.isBlank()) {
            Consumer<MessageEnvelope> callback = callbacks.remove(correlationId); // lấy callback ra và xóa khỏi map
            if (callback != null) {
                callback.accept(envelope); // gọi hàm callback đã đky
                return;
            }
        }
        switch (envelope.getType()) {
            case BID_UPDATED_EVENT -> {
                if (onBidUpdated != null) {
                    BidUpdatedEventPayload payload = mapper.parsePayload(envelope, BidUpdatedEventPayload.class); //chuyển thành obj
                    onBidUpdated.accept(payload); //chạy onBidUpdated
                }
            }

            case AUCTION_CLOSED_EVENT -> {
                if (onAuctionClosed != null) {
                    AuctionClosedEventPayload payload = mapper.parsePayload(envelope, AuctionClosedEventPayload.class); // chuyển thành obj
                    onAuctionClosed.accept(payload); //chạy onAuctionClosed
                }
            }

            case ERROR_RES -> {
                if (onError != null) {
                    ErrorPayload payload = mapper.parsePayload(envelope, ErrorPayload.class); // chuyển thành obj
                    onError.accept(payload); // chạy onError
                }
            }
            default -> System.err.println("Unhandled type: " + envelope.getType());
        }
    }

    // đki callback cho req cụ thể
    public void onResponse(String messageId, Consumer<MessageEnvelope> callback) {
        callbacks.put(messageId, callback);
    }

    // đki hàm xử lý khi có thông báo bid mới
    public void setOnBidUpdated(Consumer<BidUpdatedEventPayload> handler) {
        this.onBidUpdated = handler;
    }

    // đki hàm xử lý khi auction kết thúc.
    public void setOnAuctionClosed(Consumer<AuctionClosedEventPayload> handler) {
        this.onAuctionClosed = handler;
    }

    // đki ký hàm xử lý khi server gửi lỗi.
    public void setOnError(Consumer<ErrorPayload> handler) {
        this.onError = handler;
    }
}
