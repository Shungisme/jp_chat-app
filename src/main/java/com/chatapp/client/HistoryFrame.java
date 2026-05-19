package com.chatapp.client;

import com.chatapp.util.HistoryManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class HistoryFrame extends JFrame {
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final String me;
    private final String peer;

    public HistoryFrame(String me, String peer) {
        super("Lịch sử chat với " + peer);
        this.me = me;
        this.peer = peer;
        setSize(560, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(4, 4));

        reload();
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton delete = new JButton("Xoá dòng đã chọn");
        JButton clear = new JButton("Xoá toàn bộ");
        actions.add(delete);
        actions.add(clear);
        add(actions, BorderLayout.SOUTH);

        delete.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i >= 0 && HistoryManager.deleteAt(me, peer, i)) reload();
        });
        clear.addActionListener(e -> {
            HistoryManager.clear(me, peer);
            reload();
        });
    }

    private void reload() {
        model.clear();
        List<String> lines = HistoryManager.load(me, peer);
        for (String l : lines) model.addElement(l);
    }
}
