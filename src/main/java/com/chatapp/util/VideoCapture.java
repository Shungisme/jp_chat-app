package com.chatapp.util;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

// Reflection-based wrapper around com.github.sarxos.webcam.Webcam so the
// project still compiles when the Sarxos jar is not on the classpath.
// Build with `mvn package` to pull the dependency in.
public class VideoCapture {
    public static final Dimension FRAME_SIZE = new Dimension(320, 240);
    public static final float JPEG_QUALITY = 0.6f;

    private Object webcam;
    private Method getImageMethod;
    private Method closeMethod;

    public static boolean isAvailable() {
        try {
            Class.forName("com.github.sarxos.webcam.Webcam");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean open() {
        try {
            Class<?> webcamCls = Class.forName("com.github.sarxos.webcam.Webcam");
            Method getDefault = webcamCls.getMethod("getDefault");
            webcam = getDefault.invoke(null);
            if (webcam == null) return false;
            Method setViewSize = webcamCls.getMethod("setViewSize", Dimension.class);
            setViewSize.invoke(webcam, FRAME_SIZE);
            Method openM = webcamCls.getMethod("open");
            openM.invoke(webcam);
            getImageMethod = webcamCls.getMethod("getImage");
            closeMethod = webcamCls.getMethod("close");
            return true;
        } catch (Throwable t) {
            System.err.println("[Video] open failed: " + t.getMessage());
            return false;
        }
    }

    public BufferedImage grab() {
        if (webcam == null || getImageMethod == null) return null;
        try {
            return (BufferedImage) getImageMethod.invoke(webcam);
        } catch (Throwable t) {
            return null;
        }
    }

    public void close() {
        try {
            if (closeMethod != null && webcam != null) closeMethod.invoke(webcam);
        } catch (Throwable ignored) {}
        webcam = null;
        getImageMethod = null;
        closeMethod = null;
    }

    public static byte[] encodeJpeg(BufferedImage image) {
        if (image == null) return null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "JPEG", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static BufferedImage decodeJpeg(byte[] data) {
        if (data == null || data.length == 0) return null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            return ImageIO.read(bis);
        } catch (Exception e) {
            return null;
        }
    }
}
