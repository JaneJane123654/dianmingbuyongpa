package com.classroomassistant.core.speech;

/**
 * 唤醒词检测器接口（KWS - Keyword Spotting）
 */
public interface WakeWordDetector {
    
    /**
     * 开始检测
     * @param listener 唤醒词监听器
     */
    void startDetection(WakeWordListener listener);
    
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
     * 是否正在检测
     */
    boolean isDetecting();
    
    /**
     * 设置唤醒词
     * @param keywords 唤醒词列表
     */
    void setKeywords(String... keywords);
}
