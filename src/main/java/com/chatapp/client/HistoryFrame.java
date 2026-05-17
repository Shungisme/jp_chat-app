package com.chatapp.client;

import com.chatapp.util.HistoryManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class HistoryFrame extends JFrame {
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);

    public HistoryFrame(String me, String peer) {
        super("Lịch sử chat với " + peer);
        setSize(560, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        List<String> lines = HistoryManager.load(me, peer);
        for (String l : lines) model.addElement(l);
        add(new JScrollPane(list), BorderLayout.CENTER);
    }
}
