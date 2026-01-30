package com.classroomassistant.speech;

import java.nio.file.Path;

/**
 * 安静检测器接口
 */
public interface SilenceDetector {

    /**
     * 初始化检测器
     *
     * @param modelFile 模型文件
     */
    void initialize(Path modelFile);

    /**
     * 检测安静
     *
     * @param frame 音频帧（浮点）
     * @param durationMillis 帧时长（毫秒）
     * @return 是否检测到安静
     */
    boolean detect(float[] frame, int durationMillis);

    /**
     * 设置安静阈值
     *
     * @param seconds 阈值（秒）
     */
    void setQuietThresholdSeconds(int seconds);

    /**
     * 添加监听器
     *
     * @param listener 监听器
     */
    void addListener(SilenceListener listener);
}
