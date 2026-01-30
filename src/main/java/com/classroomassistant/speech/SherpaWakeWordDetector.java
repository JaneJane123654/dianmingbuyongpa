package com.classroomassistant.speech;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sherpa-ONNX 唤醒词检测器实现
 */
public class SherpaWakeWordDetector implements WakeWordDetector, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(SherpaWakeWordDetector.class);

    private final SherpaOnnxJNI jni = new SherpaOnnxJNI();
    private final List<WakeWordListener> listeners = new CopyOnWriteArrayList<>();

    private volatile long handle;

    @Override
    public void initialize(Path modelDir, String keywords) {
        Objects.requireNonNull(modelDir, "模型目录不能为空");
        SherpaOnnxJNI.ensureLoaded();
        handle = jni.initializeKws(modelDir, keywords == null ? "" : keywords);
        logger.info("唤醒词检测器初始化完成，模型目录: {}", modelDir);
    }

    @Override
    public boolean detect(float[] frame) {
        if (handle == 0 || frame == null || frame.length == 0) {
            return false;
        }
        boolean detected = jni.detectWakeWord(handle, frame, frame.length);
        if (detected) {
            notifyDetected();
        }
        return detected;
    }

    @Override
    public void addListener(WakeWordListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void notifyDetected() {
        for (WakeWordListener listener : listeners) {
            listener.onWakeWordDetected("检测到唤醒词");
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
