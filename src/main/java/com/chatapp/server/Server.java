package com.chatapp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final int PORT = 9999;

    private ServerSocket serverSocket;
    private volatile boolean running;

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("[Server] listening on port " + PORT);
        while (running) {
            Socket client = serverSocket.accept();
            System.out.println("[Server] accepted " + client.getRemoteSocketAddress());
        }
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null) serverSocket.close();
    }

    public static void main(String[] args) throws IOException {
        new Server().start();
    }
}
