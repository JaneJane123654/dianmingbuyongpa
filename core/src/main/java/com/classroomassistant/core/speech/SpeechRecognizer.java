package com.classroomassistant.core.speech;

/**
 * 语音识别器接口（ASR）
 */
public interface SpeechRecognizer {
    
    /**
     * 开始识别
     * @param listener 识别结果监听器
     */
    void startRecognition(RecognitionListener listener);
    
    /**
     * 输入音频数据
     * @param audioData PCM 音频数据
     * @param length 数据长度
     */
    void feedAudio(byte[] audioData, int length);
    
    /**
     * 停止识别
     */
    void stopRecognition();
    
    /**
     * 释放资源
     */
    void release();
    
    /**
     * 是否正在识别
     */
    boolean isRecognizing();
}
