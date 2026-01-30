package com.classroomassistant.speech;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sherpa-ONNX 安静检测实现
 */
public class SherpaSilenceDetector implements SilenceDetector, AutoCloseable {

    private final SherpaOnnxJNI jni = new SherpaOnnxJNI();
    private final List<SilenceListener> listeners = new CopyOnWriteArrayList<>();
    private volatile long handle;
    private int quietThresholdSeconds = 5;
    private int quietMillis;

    @Override
    public void initialize(Path modelFile) {
        SherpaOnnxJNI.ensureLoaded();
        handle = jni.initializeVad(modelFile);
    }

    @Override
    public boolean detect(float[] frame, int durationMillis) {
        if (handle == 0 || frame == null || frame.length == 0) {
            return false;
        }
        boolean hasSpeech = jni.detectSpeech(handle, frame, frame.length);
        if (hasSpeech) {
            quietMillis = 0;
            return true;
        }
        quietMillis += Math.max(0, durationMillis);
        if (quietMillis >= quietThresholdSeconds * 1000L) {
            quietMillis = 0;
            notifyTimeout();
        }
        return false;
    }

    @Override
    public void setQuietThresholdSeconds(int seconds) {
        if (seconds > 0) {
            this.quietThresholdSeconds = seconds;
        }
    }

    @Override
    public void addListener(SilenceListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void notifyTimeout() {
        for (SilenceListener listener : listeners) {
            listener.onSilenceTimeout();
        }
    }

    @Override
    public void close() {
        if (handle != 0) {
            jni.release(handle);
            handle = 0;
        }
    }
}
