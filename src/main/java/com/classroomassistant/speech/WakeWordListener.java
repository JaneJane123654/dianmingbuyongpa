package com.classroomassistant.speech;

/**
 * 唤醒词监听器 (Wake Word Listener)
 *
 * <p>用于接收由 {@link WakeWordDetector} 触发的唤醒词识别通知。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface WakeWordListener {

    /**
     * 当识别引擎检测到预设的唤醒词时触发
     *
     * @param keyword 实际检测到的唤醒词内容
     */
    void onWakeWordDetected(String keyword);
}
