package com.classroomassistant.speech;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sherpa-ONNX 本地接口 (Sherpa-ONNX JNI Bridge)
 *
 * <p>该类作为 Java 与底层 C++ (Sherpa-ONNX) 库之间的桥梁，定义了所有的 Native 方法。
 * 核心功能包括：
 * <ul>
 *   <li>KWS (Keyword Spotting): 唤醒词/关键词检测。</li>
 *   <li>VAD (Voice Activity Detection): 静音检测/人声活动检测。</li>
 *   <li>ASR (Automatic Speech Recognition): 语音转文本识别。</li>
 * </ul>
 *
 * <p>注意：该类采用延迟加载策略，仅在首次调用 {@link #ensureLoaded()} 时尝试加载本地动态链接库 (DLL/SO)。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class SherpaOnnxJNI {

    private static final Logger logger = LoggerFactory.getLogger(SherpaOnnxJNI.class);
    private static final String[] LIB_NAMES = {
        "sherpa-onnx-jni",
        "sherpa-onnx-jni-android",
        "sherpa-onnx"
    };

    private static volatile boolean loaded;

    /**
     * 确保本地动态库已成功加载
     *
     * <p>如果加载失败，将抛出 {@link UnsatisfiedLinkError}。
     * 该方法是线程安全的。
     */
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
                    logger.info("Sherpa-ONNX JNI 库加载完成: {}", libName);
                    return;
                } catch (UnsatisfiedLinkError e) {
                    lastError = e;
                }
            }
            String lastMessage = lastError == null ? "未知错误" : lastError.getMessage();
            logger.warn("Sherpa-ONNX JNI 库加载失败: {}", lastMessage);
            if (lastError != null) {
                throw lastError;
            }
            throw new UnsatisfiedLinkError(lastMessage);
        }
    }

    /**
     * 初始化唤醒词检测引擎 (KWS)
     *
     * @param modelDir 包含 KWS 模型的目录路径
     * @param keywords 要监听的关键词列表（通常为逗号分隔）
     * @return Native 对象的内存句柄 (Handle)，失败返回 0
     */
    public native long initializeKws(Path modelDir, String keywords);

    /**
     * 在音频数据中检测唤醒词
     *
     * @param handle     Native 句柄
     * @param audioData  浮点型 PCM 数据 (Normalised to [-1, 1])
     * @param numSamples 采样点数
     * @return true 表示检测到唤醒词
     */
    public native boolean detectWakeWord(long handle, float[] audioData, int numSamples);

    /**
     * 初始化静音检测引擎 (VAD)
     *
     * @param modelFile VAD 模型文件的绝对路径
     * @return Native 对象的内存句柄 (Handle)，失败返回 0
     */
    public native long initializeVad(Path modelFile);

    /**
     * 在音频数据中检测是否存在人声活动
     *
     * @param handle     Native 句柄
     * @param audioData  浮点型 PCM 数据 (Normalised to [-1, 1])
     * @param numSamples 采样点数
     * @return true 表示检测到人声活动（非静音）
     */
    public native boolean detectSpeech(long handle, float[] audioData, int numSamples);

    /**
     * 初始化语音识别引擎 (ASR)
     *
     * @param modelDir 包含 ASR 模型和配置的目录路径
     * @return Native 对象的内存句柄 (Handle)，失败返回 0
     */
    public native long initializeAsr(Path modelDir);

    /**
     * 识别 PCM 音频数据并返回文本
     *
     * @param handle Native 句柄
     * @param pcm    原始 16-bit PCM 字节数组
     * @return 识别出的文本内容
     */
    public native String recognize(long handle, byte[] pcm);

    /**
     * 释放 Native 对象内存
     *
     * @param handle 要释放的 Native 句柄
     */
    public native void release(long handle);
}
