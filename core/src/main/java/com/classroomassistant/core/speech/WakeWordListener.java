package com.classroomassistant.core.speech;

/**
 * 唤醒词检测监听器
 */
public interface WakeWordListener {
    
    /**
     * 检测到唤醒词
     * @param keyword 检测到的唤醒词
     * @param confidence 置信度 (0.0 ~ 1.0)
     */
    void onWakeWordDetected(String keyword, float confidence);
}
