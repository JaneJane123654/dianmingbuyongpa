package com.classroomassistant.speech;

/**
 * 语音识别结果监听器 (Speech Recognition Result Listener)
 *
 * <p>用于接收 {@link SpeechRecognizer} 异步识别后的结果或异常通知。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface RecognitionListener {

    /**
     * 当识别引擎成功生成文本结果时触发
     *
     * @param text 识别出的原始文本内容。如果无法识别，可能为空字符串。
     */
    void onResult(String text);

    /**
     * 当识别过程中发生异常时触发
     *
     * @param error 错误描述信息（如模型未加载、JNI 调用失败等）
     */
    void onError(String error);
}
