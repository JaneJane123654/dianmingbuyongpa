package com.classroomassistant.core.speech;

/**
 * 静音检测器接口（VAD - Voice Activity Detection）
 */
public interface SilenceDetector {
    
    /**
     * 开始检测
     * @param listener 静音监听器
     */
    void startDetection(SilenceListener listener);
    
    /**
     * 输入音频数据
     * @param audioData PCM 音频数据
     * @param length 数据长度
     */
    void feedAudio(byte[] audioData, int length);
    
    /**
     * 停止检测
     */
    void stopDetection();
    
    /**
     * 释放资源
     */
    void release();
    
    /**
     * 设置静音超时阈值
     * @param timeoutMs 静音超时时间（毫秒）
     */
    void setSilenceTimeout(long timeoutMs);
    
    /**
     * 设置静音阈值（能量阈值）
     * @param threshold 能量阈值 (0.0 ~ 1.0)
     */
    void setSilenceThreshold(float threshold);
}
