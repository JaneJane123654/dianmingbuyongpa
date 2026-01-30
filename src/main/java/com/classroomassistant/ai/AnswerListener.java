package com.classroomassistant.ai;

/**
 * AI 回答监听器
 */
public interface AnswerListener {

    /**
     * 接收流式 Token
     *
     * @param token 内容片段
     */
    void onToken(String token);

    /**
     * 回答完成
     *
     * @param answer 完整回答
     */
    void onComplete(String answer);

    /**
     * 处理异常
     *
     * @param error 错误描述
     */
    void onError(String error);
}
