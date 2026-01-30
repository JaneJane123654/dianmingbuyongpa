package com.classroomassistant.speech;

/**
 * 唤醒词监听器
 */
public interface WakeWordListener {

    /**
     * 唤醒词命中回调
     *
     * @param keyword 命中的关键词
     */
    void onWakeWordDetected(String keyword);
}
