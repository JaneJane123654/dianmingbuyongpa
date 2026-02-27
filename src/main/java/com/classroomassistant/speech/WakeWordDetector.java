package com.classroomassistant.speech;

import java.nio.file.Path;

/**
 * 唤醒词检测器接口 (Wake Word Detector Interface)
 *
 * <p>用于实时检测音频流中的特定唤醒词（如“老师您好”）。
 * 通常采用轻量级的深度学习模型实现，以保证在后台低功耗运行。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface WakeWordDetector {

    /**
     * 初始化唤醒词检测引擎
     *
     * @param modelDir 模型文件所在的目录路径（绝对路径）
     * @param keywords 要监听的唤醒词关键词，多个词之间通常用逗号分隔
     * @throws RuntimeException 如果模型文件加载失败
     */
    void initialize(Path modelDir, String keywords);

    /**
     * 在给定的音频帧中检测唤醒词
     *
     * @param frame 归一化后的浮点型音频帧数据（通常为 16kHz, 单声道）
     * @return true 表示检测到唤醒词，false 表示未检测到
     */
    boolean detect(float[] frame);

    /**
     * 注册唤醒词监听器
     * <p>当检测到唤醒词时，将通过此监听器进行回调。
     *
     * @param listener 唤醒词监听器实例 {@link WakeWordListener}
     */
    void addListener(WakeWordListener listener);
}
