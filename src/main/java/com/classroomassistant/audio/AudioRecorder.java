package com.classroomassistant.audio;

/**
 * 音频录制器接口
 *
 * <p>负责音频采集、回溯读取与监听器通知。
 */
public interface AudioRecorder extends AutoCloseable {

    /**
     * 启动录音
     */
    void startRecording();

    /**
     * 停止录音
     */
    void stopRecording();

    /**
     * 是否正在录音
     *
     * @return true 表示录音中
     */
    boolean isRecording();

    /**
     * 获取前 N 秒的音频数据
     *
     * @param seconds 回溯秒数（1-300）
     * @return PCM 音频数据
     */
    byte[] getAudioBefore(int seconds);

    /**
     * 添加音频监听器
     *
     * @param listener 监听器
     */
    void addListener(AudioListener listener);

    /**
     * 释放资源
     */
    @Override
    void close();
}
