package com.classroomassistant.core.speech;

/**
 * 静音检测监听器
 */
public interface SilenceListener {
    
    /**
     * 检测到静音开始
     */
    void onSilenceStart();
    
    /**
     * 检测到静音结束（有声音输入）
     */
    void onSilenceEnd();
    
    /**
     * 静音超时（持续静音超过阈值）
     * @param durationMs 静音持续时间（毫秒）
     */
    void onSilenceTimeout(long durationMs);
}
