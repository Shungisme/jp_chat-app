package com.chatapp.client;

import com.chatapp.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MessageDispatcher implements Consumer<Message> {
    private final List<Consumer<Message>> listeners = new ArrayList<>();

    public void subscribe(Consumer<Message> listener) {
        listeners.add(listener);
    }

    @Override
    public void accept(Message msg) {
        for (Consumer<Message> l : listeners) l.accept(msg);
    }
}
