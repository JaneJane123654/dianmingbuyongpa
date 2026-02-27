package com.classroomassistant.speech;

import java.nio.file.Path;

/**
 * 静音检测器接口 (Silence Detector Interface)
 *
 * <p>用于检测音频流中的静音或长段停顿。通常用于 VAD (Voice Activity Detection) 场景，
 * 以确定用户何时停止说话，从而触发语音识别或 AI 处理。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface SilenceDetector {

    /**
     * 初始化静音检测引擎
     *
     * @param modelFile VAD 模型文件的路径
     * @throws RuntimeException 如果模型文件不存在或无法加载
     */
    void initialize(Path modelFile);

    /**
     * 在给定的音频帧中检测是否为静音
     *
     * @param frame          归一化后的浮点型音频帧数据
     * @param durationMillis 该音频帧代表的时间长度（毫秒）
     * @return true 表示当前帧被判定为静音，false 表示有语音活动
     */
    boolean detect(float[] frame, int durationMillis);

    /**
     * 设置触发静音状态的时间阈值
     * <p>例如，如果设置为 3 秒，则表示只有在连续检测到 3 秒静音后才触发监听器回调。
     *
     * @param seconds 阈值秒数
     */
    void setQuietThresholdSeconds(int seconds);

    /**
     * 注册静音状态监听器
     *
     * @param listener 静音监听器实例 {@link SilenceListener}
     */
    void addListener(SilenceListener listener);
}
