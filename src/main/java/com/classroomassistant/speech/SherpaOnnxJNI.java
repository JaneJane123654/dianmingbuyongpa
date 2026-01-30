package com.classroomassistant.speech;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sherpa-ONNX JNI 接口声明
 *
 * <p>仅在实际调用时加载本地库，避免初始化阶段崩溃。
 */
public class SherpaOnnxJNI {

    private static final Logger logger = LoggerFactory.getLogger(SherpaOnnxJNI.class);
    private static final String LIB_NAME = "sherpa-onnx-jni";

    private static volatile boolean loaded;

    /**
     * 加载本地库
     */
    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (SherpaOnnxJNI.class) {
            if (loaded) {
                return;
            }
            try {
                System.loadLibrary(LIB_NAME);
                loaded = true;
                logger.info("Sherpa-ONNX JNI 库加载完成: {}", LIB_NAME);
            } catch (UnsatisfiedLinkError e) {
                logger.warn("Sherpa-ONNX JNI 库加载失败: {}", e.getMessage());
                throw e;
            }
        }
    }

    public native long initializeKws(Path modelDir, String keywords);

    public native boolean detectWakeWord(long handle, float[] audioData, int numSamples);

    public native long initializeVad(Path modelFile);

    public native boolean detectSpeech(long handle, float[] audioData, int numSamples);

    public native long initializeAsr(Path modelDir);

    public native String recognize(long handle, byte[] pcm);

    public native void release(long handle);
}
