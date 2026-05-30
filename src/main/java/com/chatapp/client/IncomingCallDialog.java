package com.chatapp.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class IncomingCallDialog extends JDialog {

    public IncomingCallDialog(Frame parent, String from, String callType,
                              Runnable onAccept, Runnable onReject) {
        super(parent, "Cuộc gọi " + callType + " đến", false);
        setSize(360, 170);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(8, 8));

        JLabel msg = new JLabel("<html><div style='text-align:center;'>"
                + "<b>" + from + "</b> đang gọi " + callType + " bạn.</div></html>",
                SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msg.setBorder(BorderFactory.createEmptyBorder(20, 16, 8, 16));
        add(msg, BorderLayout.CENTER);

        JButton accept = new JButton("Chấp nhận");
        JButton reject = new JButton("Từ chối");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.add(accept);
        buttons.add(reject);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        add(buttons, BorderLayout.SOUTH);

        accept.addActionListener(e -> { dispose(); onAccept.run(); });
        reject.addActionListener(e -> { dispose(); onReject.run(); });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onReject.run(); }
        });

        toFront();
        setAlwaysOnTop(true);
    }
}
