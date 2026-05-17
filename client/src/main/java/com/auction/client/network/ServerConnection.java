package com.auction.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    private ServerConnection(){};

    public static synchronized ServerConnection getInstance(){
        if(instance==null) instance = new ServerConnection();
        return instance;
    }
    // knoi tới server và gửi/nhận dlieu
    public void connect(String host, int port) throws IOException{
        if(connected) return;
        socket = new Socket(host, port); // kết nối tới server
        out = new PrintWriter(socket.getOutputStream(), true); //gửi dlieu đến server
        in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // nhận dlieu từ server
        connected=true;
        System.out.println("Đã kết nối đến " + host + ":" + port);
    }
    // ngắt knoi server, đóng luồng/socket
    public void disconnect(){
        connected = false;
        try{
            if(in != null) in.close();
            if(out!= null) out.close();
            if(socket!=null && !socket.isClosed()) socket.close();
        }catch (IOException e){
            System.out.println("Đã ngắt kết nối");
        }
    }
    // lấy luồng ghi dlieu
    public PrintWriter getOut() {
        if (!connected) throw new IllegalStateException("Chưa kết nối đến server");
        return out;
    }
    // lấy luồng nhận dlieu
    public BufferedReader getIn() {
        if (!connected) throw new IllegalStateException("Chưa kết nối đến server");
        return in;
    }

    public boolean isConnected() { return connected; }

}
