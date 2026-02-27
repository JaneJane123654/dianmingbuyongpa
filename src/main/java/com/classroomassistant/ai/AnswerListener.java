package com.classroomassistant.ai;

/**
 * AI 回答监听器
 *
 * <p>用于异步获取 AI 生成的内容，支持流式输出和完成回调。
 *
 * <p>使用示例：
 * <pre>
 * llmClient.generateAnswerAsync("你好", new AnswerListener() {
 *     @Override
 *     public void onToken(String token) {
 *         System.out.print(token);
 *     }
 *     @Override
 *     public void onComplete(String answer) {
 *         System.out.println("\n回答完毕");
 *     }
 *     @Override
 *     public void onError(String error) {
 *         System.err.println("发生错误: " + error);
 *     }
 * });
 * </pre>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface AnswerListener {

    /**
     * 接收流式 Token
     *
     * @param token 内容片段，由 AI 模型逐步返回
     */
    void onToken(String token);

    /**
     * 回答完成
     *
     * @param answer 完整回答内容，包含所有已收到的 Token
     */
    void onComplete(String answer);

    /**
     * 处理异常
     *
     * @param error 错误描述或异常信息
     */
    void onError(String error);
}
