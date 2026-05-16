package com.auction.server.network;

import com.auction.server.service.AuctionClosingService;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AuctionServiceImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AuctionServer {
    private static final int defaultPort = 1337; //port mặc định
    private static final int threadPollSize = 50; //xly tối đa 50 client cùng lúc
    private final int port;
    private final ExecutorService threadPool; // qly thread
    private final RequestDispatcher dispatcher; // pphoi req đến đúng handler
    private final AuctionClosingService closingService; // ktra phiên hết hạn để đóng

    private ServerSocket serverSocket; // mở cổng/chờ client knoi
    private volatile boolean running = false; // biến cờ cho bt server còn chạy ko

    public AuctionServer(int port){
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(threadPollSize); // threadPoll tối đa 50 thread
        AuctionService auctionService = new AuctionServiceImpl();
        this.dispatcher = new RequestDispatcher();
        this.closingService = new AuctionClosingService(auctionService);
    }

    public AuctionServer() {
        this(defaultPort);
    }

    // khời động server
    public void start() throws IOException{
        serverSocket = new ServerSocket(port); // mở cổng
        running = true;
        closingService.start(); // start thread nền ktra thread hết hạn
        System.out.println("Auction Server đang chạy");
        System.out.println("Port: " + port + " ");
        while (running){
            try{
                Socket clientSocket = serverSocket.accept(); // server chờ cho đến khi có knoi khi có thì trả về Socket
                ClientConnectionHandler handler = new ClientConnectionHandler(clientSocket,dispatcher); //xly giao tiếp
                threadPool.submit(handler); // gửi vào threadPool
            }catch (IOException e){
                if(running){
                    System.err.println("Lỗi chấp nhận kết nối: " + e.getMessage());
                }
            }
        }
    }

    // dừng toàn bộ server
    public void stop() {
        running = false; // dừng vòng lặp
        closingService.stop();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {
        }
        threadPool.shutdown(); // ngừng nhận việc mới
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) threadPool.shutdownNow(); // chờ tối đa 5s
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt(); // đánh dấu là thread đã bị yc dừng
        }
        System.out.println("Server đã dừng");
    }

    public boolean isRunning(){
        return running;
    }

    public int getPort(){
        return port;
    }

}
