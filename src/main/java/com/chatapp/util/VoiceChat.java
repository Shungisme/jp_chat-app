package com.chatapp.util;

import javax.sound.sampled.*;
import java.util.function.Consumer;

public class VoiceChat {
    public static final AudioFormat FORMAT = new AudioFormat(8000f, 16, 1, true, false);
    private static final int BUFFER_SIZE = 1600;

    private volatile TargetDataLine micLine;
    private volatile SourceDataLine speakerLine;
    private Thread captureThread;
    private volatile boolean capturing;

    public void startPlayback() throws LineUnavailableException {
        if (speakerLine != null) return;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
        speakerLine = (SourceDataLine) AudioSystem.getLine(info);
        speakerLine.open(FORMAT);
        speakerLine.start();
    }

    public void play(byte[] chunk) {
        SourceDataLine line = speakerLine;
        if (line != null && chunk != null && chunk.length > 0) {
            try {
                line.write(chunk, 0, chunk.length);
            } catch (Exception ignored) {
                // line was closed by stop() between the null-check and write
            }
        }
    }

    public void startCapture(Consumer<byte[]> chunkSink) throws LineUnavailableException {
        if (capturing) return;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        micLine = (TargetDataLine) AudioSystem.getLine(info);
        micLine.open(FORMAT);
        micLine.start();
        capturing = true;
        final TargetDataLine localLine = micLine;
        captureThread = new Thread(() -> {
            byte[] buf = new byte[BUFFER_SIZE];
            try {
                while (capturing) {
                    int n = localLine.read(buf, 0, buf.length);
                    if (n <= 0) break;
                    byte[] chunk = new byte[n];
                    System.arraycopy(buf, 0, chunk, 0, n);
                    chunkSink.accept(chunk);
                }
            } catch (Exception ignored) {
                // line was closed by stopCapture() while we were blocked on read()
            }
        }, "voice-capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public synchronized void stopCapture() {
        capturing = false;
        TargetDataLine line = micLine;
        Thread t = captureThread;
        micLine = null;
        captureThread = null;
        if (line != null) {
            line.stop();
            line.close();
        }
        if (t != null) {
            try {
                t.join(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void stop() {
        stopCapture();
        SourceDataLine line = speakerLine;
        speakerLine = null;
        if (line != null) {
            line.stop();
            line.close();
        }
    }
}
