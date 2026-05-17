package com.auction.server.network;

import java.io.IOException;

public class ServerApplication {
    public static void main(String[] args) {
        int port = 1337;
        AuctionServer server = new AuctionServer(port);
        try{
            System.out.println("Starting Auction Server...");
            server.start();
        } catch (Exception e) {
            System.err.println("Server failed to start: " + e.getMessage());
            System.exit(1); // có lỗi dừng toàn bộ chương trình
        }
    }
}
