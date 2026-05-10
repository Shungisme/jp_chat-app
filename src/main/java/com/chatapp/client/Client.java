package com.chatapp.client;

import com.chatapp.model.Message;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Client {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String username;
    private Consumer<Message> listener;
    private Thread reader;
    private volatile boolean running;
    private final ConcurrentLinkedQueue<Message> outgoing = new ConcurrentLinkedQueue<>();
    private final Object writeLock = new Object();

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect(String username) throws IOException {
        this.username = username;
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        running = true;
        reader = new Thread(this::readLoop, "client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        try {
            while (running) {
                Object obj = in.readObject();
                if (obj instanceof Message m && listener != null) listener.accept(m);
            }
        } catch (Exception e) {
            if (running) System.err.println("[Client] reader stopped: " + e.getMessage());
        }
    }

    public void setListener(Consumer<Message> listener) {
        this.listener = listener;
    }

    // Writes are serialized to preserve send order.
    public void send(Message msg) throws IOException {
        msg.setSender(username);
        synchronized (writeLock) {
            out.writeObject(msg);
            out.flush();
        }
    }

    public void close() throws IOException {
        running = false;
        if (socket != null) socket.close();
    }

    public String getUsername() { return username; }
}
