package com.chatapp.client;

import com.chatapp.model.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;
    private Consumer<Message> listener;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect(String username) throws IOException {
        this.username = username;
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void setListener(Consumer<Message> listener) {
        this.listener = listener;
    }

    public void send(Message msg) throws IOException {
        msg.setSender(username);
        out.writeObject(msg);
        out.flush();
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
    }

    public String getUsername() { return username; }
}
