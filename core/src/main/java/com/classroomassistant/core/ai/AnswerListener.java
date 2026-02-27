package com.classroomassistant.core.ai;

/**
 * AI 回答监听器
 * 
 * 用于异步获取 AI 生成的内容，支持流式输出和完成回调。
 */
public interface AnswerListener {

    /**
     * 接收到一个 token（流式输出）
     * @param token 生成的文本片段
     */
    void onToken(String token);

    /**
     * 回答生成完毕
     * @param answer 完整的回答内容
     */
    void onComplete(String answer);

    /**
     * 生成过程出错
     * @param error 错误信息
     */
    void onError(String error);
}
