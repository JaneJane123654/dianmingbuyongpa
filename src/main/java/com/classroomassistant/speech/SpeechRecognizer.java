package com.classroomassistant.speech;

import java.nio.file.Path;

/**
 * 语音识别器接口
 */
public interface SpeechRecognizer {

    /**
     * 初始化识别器
     *
     * @param modelDir 模型目录
     */
    void initialize(Path modelDir);

    /**
     * 同步识别
     *
     * @param pcm PCM 数据
     * @return 识别文本
     */
    String recognize(byte[] pcm);

    /**
     * 异步识别
     *
     * @param pcm PCM 数据
     * @param listener 监听器
     */
    void recognizeAsync(byte[] pcm, RecognitionListener listener);
}
