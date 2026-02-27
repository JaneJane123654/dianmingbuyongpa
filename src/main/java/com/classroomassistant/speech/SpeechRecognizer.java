package com.classroomassistant.speech;

import java.nio.file.Path;

/**
 * 语音识别器接口 (Speech Recognizer Interface)
 *
 * <p>定义了将音频数据（PCM 格式）转换为文本（ASR - Automatic Speech Recognition）的标准接口。
 * 支持同步识别和异步流式识别。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface SpeechRecognizer {

    /**
     * 初始化语音识别引擎
     *
     * @param modelDir 模型文件所在的目录路径
     * @throws RuntimeException 如果模型文件不存在或加载失败
     */
    void initialize(Path modelDir);

    /**
     * 同步识别音频数据
     *
     * @param pcm 原始 PCM 音频数据字节数组
     * @return 识别出的文本结果。如果无法识别，可能返回空字符串。
     */
    String recognize(byte[] pcm);

    /**
     * 异步识别音频数据
     * <p>适用于较长音频或需要非阻塞处理的场景。
     *
     * @param pcm      原始 PCM 音频数据字节数组
     * @param listener 接收识别结果的回调监听器 {@link RecognitionListener}
     */
    void recognizeAsync(byte[] pcm, RecognitionListener listener);
}
