package com.classroomassistant.audio;

/**
 * 音频监听器
 *
 * <p>用于接收采集到的音频帧与错误通知。
 */
public interface AudioListener {

    /**
     * 接收音频帧数据
     *
     * @param data PCM 音频帧数据，16-bit 小端序
     */
    void onAudioReady(byte[] data);

    /**
     * 接收错误信息
     *
     * @param error 错误描述
     */
    void onError(String error);
}
