package com.classroomassistant.speech;

import java.nio.file.Path;

public class SherpaOnnxJNI {
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
                    return;
                } catch (UnsatisfiedLinkError e) {
                    lastError = e;
                }
            }
            if (lastError != null) {
                throw lastError;
            }
            throw new UnsatisfiedLinkError("Failed to load Sherpa-ONNX JNI library");
        }
    }

    public native long initializeKws(Path modelDir, String keywords);

    public native boolean detectWakeWord(long handle, float[] audioData, int numSamples);

    public native void release(long handle);
}
