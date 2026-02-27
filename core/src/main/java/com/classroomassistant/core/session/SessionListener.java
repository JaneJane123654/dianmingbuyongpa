package com.classroomassistant.core.session;

/**
 * 会话事件监听器
 */
public interface SessionListener {
    
    /**
     * 状态改变
     * @param oldState 旧状态
     * @param newState 新状态
     */
    void onStateChanged(SessionState oldState, SessionState newState);
    
    /**
     * 检测到唤醒词
     * @param keyword 唤醒词
     */
    void onWakeWordDetected(String keyword);
    
    /**
     * 语音识别结果
     * @param text 识别的文本
     */
    void onSpeechRecognized(String text);
    
    /**
     * AI 回答生成中（流式）
     * @param token 当前 token
     */
    void onAnswerToken(String token);
    
    /**
     * AI 回答完成
     * @param answer 完整回答
     */
    void onAnswerComplete(String answer);
    
    /**
     * 发生错误
     * @param error 错误信息
     */
    void onError(String error);
    
    /**
     * 日志消息
     * @param message 日志内容
     */
    void onLog(String message);
}
