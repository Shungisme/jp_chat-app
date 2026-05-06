package com.chatapp.server;

import com.chatapp.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Object obj;
            while ((obj = in.readObject()) != null) {
                if (obj instanceof Message msg) {
                    System.out.println("[Server] received " + msg);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[Server] client gone: " + e.getMessage());
        }
    }

    public String getUsername() { return username; }

    public void send(Message msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }
}
