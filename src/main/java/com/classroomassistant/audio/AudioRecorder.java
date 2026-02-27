package com.classroomassistant.audio;

/**
 * 音频录制器接口 (Audio Recorder Interface)
 *
 * <p>负责实时音频采集、环形缓冲区管理（用于回溯读取）以及通知音频监听器。
 * 实现类应确保音频采集的稳定性和低延迟。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface AudioRecorder extends AutoCloseable {

    /**
     * 启动音频录制
     * <p>开启硬件采集设备并开始向内部缓冲区写入数据。
     *
     * @throws RuntimeException 如果无法打开音频输入行或采集设备
     */
    void startRecording();

    /**
     * 停止音频录制
     * <p>停止采集并释放相关硬件资源，但内部缓冲区数据可能仍可访问（取决于具体实现）。
     */
    void stopRecording();

    /**
     * 检查当前是否正在录音
     *
     * @return true 表示正在录音，false 表示已停止
     */
    boolean isRecording();

    /**
     * 获取指定时长的前序音频数据
     * <p>从环形缓冲区中提取最近 N 秒的原始 PCM 数据。常用于“唤醒”后的上下文获取。
     *
     * @param seconds 需要获取的秒数（建议范围 1-300 秒）
     * @return 原始 PCM 字节数组。如果请求长度超过缓冲区容量，则返回缓冲区内所有可用数据。
     */
    byte[] getAudioBefore(int seconds);

    /**
     * 注册音频监听器
     * <p>当有新的音频帧产生时，将通过此监听器进行回调。
     *
     * @param listener 音频监听器实例 {@link AudioListener}
     */
    void addListener(AudioListener listener);

    /**
     * 释放所有占用的音频资源
     * <p>必须调用此方法以确保系统音频设备被正确释放。
     */
    @Override
    void close();
}
