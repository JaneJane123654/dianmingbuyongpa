package com.classroomassistant.desktop.speech;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sherpa-ONNX JNI 桥接。
 */
public class SherpaOnnxJNI {

    private static final Logger logger = LoggerFactory.getLogger(SherpaOnnxJNI.class);
    private static final String[] LIB_NAMES = {
            "sherpa-onnx-jni",
            "sherpa-onnx-jni-android",
            "sherpa-onnx"
    };

    private static volatile boolean loaded;

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (SherpaOnnxJNI.class) {
            if (loaded) {
                return;
            }
            UnsatisfiedLinkError lastError = null;
            for (String libName : LIB_NAMES) {
                try {
                    System.loadLibrary(libName);
                    loaded = true;
                    logger.info("Sherpa-ONNX JNI loaded: {}", libName);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    lastError = e;
                }
            }
            if (lastError != null) {
                logger.warn("Sherpa-ONNX JNI load failed: {}", lastError.getMessage());
                throw lastError;
            }
            throw new UnsatisfiedLinkError("Failed to load Sherpa-ONNX JNI library");
        }
    }

    public native long initializeKws(Path modelDir, String keywords);

    public native boolean detectWakeWord(long handle, float[] audioData, int numSamples);

    public native long initializeAsr(Path modelDir);

    public native String recognize(long handle, byte[] pcm);

    public native void release(long handle);
}
