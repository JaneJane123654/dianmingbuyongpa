package com.classroomassistant.speech;

/**
 * 语音识别监听器
 */
public interface RecognitionListener {

    /**
     * 识别结果回调
     *
     * @param text 识别文本
     */
    void onResult(String text);

    /**
     * 识别异常回调
     *
     * @param error 错误描述
     */
    void onError(String error);
}
