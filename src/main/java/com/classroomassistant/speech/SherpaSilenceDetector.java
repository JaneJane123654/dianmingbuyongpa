package com.classroomassistant.speech;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sherpa-ONNX 静音检测 (VAD) 实现类
 *
 * <p>基于 Sherpa-ONNX 框架实现。通过 JNI 调用底层 ONNX Runtime 进行语音活动检测（VAD）。
 * 该类负责监听音频流，并根据设定的静音阈值触发超时事件。
 *
 * <p>注意：在不再使用时，务必调用 {@link #close()} 释放 JNI 持有的 native 句柄。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class SherpaSilenceDetector implements SilenceDetector, AutoCloseable {

    private final SherpaOnnxJNI jni = new SherpaOnnxJNI();
    private final List<SilenceListener> listeners = new CopyOnWriteArrayList<>();
    private volatile long handle;
    private int quietThresholdSeconds = 5;
    private int quietMillis;

    /**
     * 初始化 VAD 引擎
     *
     * @param modelFile VAD 模型文件路径 (.onnx)
     * @throws RuntimeException 如果 JNI 初始化失败
     */
    @Override
    public void initialize(Path modelFile) {
        SherpaOnnxJNI.ensureLoaded();
        handle = jni.initializeVad(modelFile);
    }

    /**
     * 检测当前音频帧是否包含人声，并计算静音时长
     *
     * @param frame          音频采样数据（单声道，float 格式）
     * @param durationMillis 该帧对应的时长（毫秒）
     * @return 如果检测到人声返回 true，否则返回 false
     */
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

    /**
     * 设置触发“安静超时”的时间阈值
     *
     * @param seconds 持续静音多少秒后触发超时
     */
    @Override
    public void setQuietThresholdSeconds(int seconds) {
        if (seconds > 0) {
            this.quietThresholdSeconds = seconds;
        }
    }

    /**
     * 注册静音状态监听器
     *
     * @param listener 监听器实例
     */
    @Override
    public void addListener(SilenceListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 触发静音超时通知
     */
    private void notifyTimeout() {
        for (SilenceListener listener : listeners) {
            listener.onSilenceTimeout();
        }
    }

    /**
     * 释放资源，销毁 JNI 句柄
     */
    @Override
    public void close() {
        if (handle != 0) {
            jni.release(handle);
            handle = 0;
        }
    }
}
