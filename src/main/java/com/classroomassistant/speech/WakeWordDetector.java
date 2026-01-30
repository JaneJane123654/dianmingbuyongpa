package com.classroomassistant.speech;

import java.nio.file.Path;

/**
 * 唤醒词检测器接口
 */
public interface WakeWordDetector {

    /**
     * 初始化检测器
     *
     * @param modelDir 模型目录（绝对路径）
     * @param keywords 关键词（逗号分隔）
     */
    void initialize(Path modelDir, String keywords);

    /**
     * 检测唤醒词
     *
     * @param frame 音频帧（浮点）
     * @return 是否命中
     */
    boolean detect(float[] frame);

    /**
     * 添加监听器
     *
     * @param listener 监听器
     */
    void addListener(WakeWordListener listener);
}
