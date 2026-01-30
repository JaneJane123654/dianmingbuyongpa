package com.classroomassistant.speech;

/**
 * 安静检测监听器
 */
public interface SilenceListener {

    /**
     * 安静超时触发
     */
    void onSilenceTimeout();
}
