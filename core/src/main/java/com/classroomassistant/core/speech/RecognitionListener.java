package com.classroomassistant.core.speech;

/**
 * 语音识别结果监听器
 */
public interface RecognitionListener {
    
    /**
     * 识别到部分结果（实时反馈）
     * @param partialText 部分识别文本
     */
    void onPartialResult(String partialText);
    
    /**
     * 识别完成
     * @param finalText 最终识别文本
     */
    void onResult(String finalText);
    
    /**
     * 识别出错
     * @param error 错误信息
     */
    void onError(String error);
}
