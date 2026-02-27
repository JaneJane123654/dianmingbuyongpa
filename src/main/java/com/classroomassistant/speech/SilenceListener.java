package com.classroomassistant.speech;

/**
 * 静音状态监听器 (Silence Listener)
 *
 * <p>用于接收由 {@link SilenceDetector} 触发的静音超时通知。
 * 该通知通常表示用户已经停止说话，可以开始执行后续的 ASR 或 AI 逻辑。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface SilenceListener {

    /**
     * 当静音持续时长超过设定的阈值时触发
     */
    void onSilenceTimeout();
}
