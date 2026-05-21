package com.chatapp.util;

import javax.sound.sampled.*;
import java.util.function.Consumer;

public class VoiceChat {
    public static final AudioFormat FORMAT = new AudioFormat(8000f, 16, 1, true, false);
    private static final int BUFFER_SIZE = 1600;

    private TargetDataLine micLine;
    private SourceDataLine speakerLine;
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
        if (speakerLine != null && chunk != null && chunk.length > 0) {
            speakerLine.write(chunk, 0, chunk.length);
        }
    }

    public void startCapture(Consumer<byte[]> chunkSink) throws LineUnavailableException {
        if (capturing) return;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        micLine = (TargetDataLine) AudioSystem.getLine(info);
        micLine.open(FORMAT);
        micLine.start();
        capturing = true;
        captureThread = new Thread(() -> {
            byte[] buf = new byte[BUFFER_SIZE];
            while (capturing) {
                int n = micLine.read(buf, 0, buf.length);
                if (n > 0) {
                    byte[] chunk = new byte[n];
                    System.arraycopy(buf, 0, chunk, 0, n);
                    chunkSink.accept(chunk);
                }
            }
        }, "voice-capture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void stopCapture() {
        capturing = false;
        if (micLine != null) {
            micLine.stop();
            micLine.close();
            micLine = null;
        }
    }

    public void stop() {
        stopCapture();
        if (speakerLine != null) {
            speakerLine.stop();
            speakerLine.close();
            speakerLine = null;
        }
    }
}
