package com.classroomassistant.audio;

/**
 * 音频数据监听器 (Audio Listener Interface)
 *
 * <p>用于接收录音设备采集到的原始音频流。实现该接口并注册到 {@link AudioRecorder}，
 * 即可实时处理音频帧数据或监控采集状态。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface AudioListener {

    /**
     * 当新的音频帧数据采集完成时触发
     *
     * @param data 原始 PCM 音频帧数据。
     *             格式通常为：16-bit 有符号，小端序（Little-Endian），采样率由配置决定。
     */
    void onAudioReady(byte[] data);

    /**
     * 当音频采集过程中发生底层硬件或系统错误时触发
     *
     * @param error 错误详情描述
     */
    void onError(String error);
}
